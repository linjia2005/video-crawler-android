package com.videocrawler

import android.content.Context
import android.os.Build
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.io.*

data class VideoInfo(
    val title: String,
    val uploader: String,
    val duration: String,
    val resolution: String,
    val size: String,
    val thumbnail: String,
    val formats: List<FormatInfo>
)

data class FormatInfo(
    val id: String,
    val label: String,
    val height: Int,
    val ext: String
)

data class DownloadTask(
    val id: String,
    val title: String,
    val url: String,
    val formatId: String,
    var status: DownloadStatus,
    var percent: Float,
    var speed: String,
    var eta: String,
    var error: String = ""
)

enum class DownloadStatus {
    PARSING, DOWNLOADING, PAUSED, COMPLETED, ERROR, CANCELLED
}

class YtDlpManager(private val context: Context) {
    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks

    private var binaryPath: String? = null
    private var runningProcesses = mutableMapOf<String, Process>()
    private var pausedFlags = mutableMapOf<String, Boolean>()
    private var cancelledFlags = mutableMapOf<String, Boolean>()

    suspend fun ensureBinary() {
        val arch = when (Build.SUPPORTED_ABIS.firstOrNull()) {
            "arm64-v8a" -> "arm64"
            "armeabi-v7a" -> "arm"
            "x86_64" -> "x86_64"
            "x86" -> "x86"
            else -> "arm64"
        }
        val fileName = "yt-dlp_android_$arch"
        val file = File(context.filesDir, fileName)

        if (file.exists()) return

        withContext(Dispatchers.IO) {
            val url = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/$fileName"
            val connection = java.net.URL(url).openConnection()
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.connectTimeout = 15000
            connection.readTimeout = 60000

            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file.setExecutable(true)
        }
        binaryPath = file.absolutePath
    }

    suspend fun parseVideo(url: String): VideoInfo? = withContext(Dispatchers.IO) {
        try {
            val bin = binaryPath ?: return@withContext null
            val process = ProcessBuilder(
                bin, "--dump-json", "--no-playlist", url
            ).redirectErrorStream(true).start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            val json = JSONObject(output)
            val formats = json.optJSONArray("formats") ?: return@withContext parseSimple(json)
            val fmtList = mutableListOf<FormatInfo>()

            for (i in 0 until formats.length()) {
                val f = formats.getJSONObject(i)
                val h = f.optInt("height", 0)
                if (h > 0 || "audio" == f.optString("vcodec", "none")) {
                    fmtList.add(FormatInfo(
                        id = f.getString("format_id"),
                        label = if (h > 0) "${h}p" else "仅音频",
                        height = h,
                        ext = f.optString("ext", "mp4")
                    ))
                }
            }
            fmtList.sortByDescending { it.height }
            val best = if (fmtList.isNotEmpty()) fmtList.first() else FormatInfo("best", "最佳", 0, "mp4")

            VideoInfo(
                title = json.optString("title", "未知"),
                uploader = json.optString("uploader", json.optString("channel", "未知")),
                duration = formatDuration(json.optInt("duration", 0)),
                resolution = if (best.height > 0) "${best.height}p" else "音频",
                size = formatSize(json.optLong("filesize", json.optLong("filesize_approx", 0))),
                thumbnail = json.optString("thumbnail", ""),
                formats = fmtList
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseSimple(json: JSONObject): VideoInfo {
        return VideoInfo(
            title = json.optString("title", "未知"),
            uploader = json.optString("uploader", "未知"),
            duration = formatDuration(json.optInt("duration", 0)),
            resolution = "${json.optInt("width", 0)}x${json.optInt("height", 0)}",
            size = formatSize(json.optLong("filesize", json.optLong("filesize_approx", 0))),
            thumbnail = json.optString("thumbnail", ""),
            formats = listOf(FormatInfo("best", "最佳画质", json.optInt("height", 0), "mp4"))
        )
    }

    fun startDownload(url: String, formatId: String, title: String) {
        val id = java.util.UUID.randomUUID().toString().take(8)
        val task = DownloadTask(id, title, url, formatId, DownloadStatus.DOWNLOADING, 0f, "", "")
        _tasks.value = _tasks.value + task
        cancelledFlags[id] = false
        pausedFlags[id] = false

        Thread {
            try {
                val bin = binaryPath ?: return@Thread
                val downloadsDir = File(context.getExternalFilesDir(null), "Downloads")
                downloadsDir.mkdirs()
                val output = "${downloadsDir.absolutePath}/%(title)s.%(ext)s"

                val pb = ProcessBuilder(
                    bin, "-f", formatId,
                    "--no-playlist",
                    "--newline", "--progress",
                    "-o", output,
                    url
                )
                pb.redirectErrorStream(false)
                val process = pb.start()
                runningProcesses[id] = process

                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (process.isAlive && !(cancelledFlags[id] ?: false)) {
                    if (pausedFlags[id] == true) {
                        Thread.sleep(500)
                        continue
                    }
                    line = reader.readLine() ?: break
                    parseProgressLine(line, id)
                }
                process.waitFor()
                val exitCode = process.exitValue()

                updateTask(id) {
                    if (cancelledFlags[id] == true) {
                        status = DownloadStatus.CANCELLED
                    } else if (exitCode == 0) {
                        status = DownloadStatus.COMPLETED
                        percent = 100f
                    } else {
                        status = DownloadStatus.ERROR
                        error = "退出码: $exitCode"
                    }
                }
            } catch (e: Exception) {
                updateTask(id) {
                    status = DownloadStatus.ERROR
                    error = e.message ?: "未知错误"
                }
            } finally {
                runningProcesses.remove(id)
                pausedFlags.remove(id)
                cancelledFlags.remove(id)
            }
        }.start()
    }

    private fun parseProgressLine(line: String, taskId: String) {
        val regex = Regex("""\[download]\s+([\d.]+)%""")
        val match = regex.find(line) ?: return
        val pct = match.groupValues[1].toFloatOrNull() ?: return
        val speed = Regex("""at\s+([\d.]+[KMG]?i?B/s])""").find(line)?.groupValues?.get(1) ?: ""
        val eta = Regex("""ETA\s+([\d:]+)""").find(line)?.groupValues?.get(1) ?: ""
        updateTask(taskId) {
            percent = pct
            this.speed = speed
            this.eta = eta
        }
    }

    private fun updateTask(id: String, block: DownloadTask.() -> Unit) {
        _tasks.value = _tasks.value.map {
            if (it.id == id) { it.apply(block) } else it
        }
    }

    fun pauseTask(id: String) { pausedFlags[id] = true }
    fun resumeTask(id: String) { pausedFlags[id] = false; updateTask(id) { status = DownloadStatus.DOWNLOADING } }
    fun cancelTask(id: String) {
        cancelledFlags[id] = true
        runningProcesses[id]?.destroyForcibly()
        runningProcesses.remove(id)
    }

    companion object {
        fun formatDuration(secs: Int): String {
            if (secs <= 0) return "未知"
            return "${secs / 60}:${secs % 60 % 60.toString().padStart(2, '0')}"
        }
        fun formatSize(bytes: Long): String {
            if (bytes <= 0) return "未知"
            val units = arrayOf("B", "KB", "MB", "GB")
            var size = bytes.toDouble()
            for (u in units) {
                if (size < 1024) return "%.1f %s".format(size, u)
                size /= 1024
            }
            return "%.1f TB".format(size)
        }
    }
}
