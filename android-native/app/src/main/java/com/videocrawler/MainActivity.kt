package com.videocrawler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

private val DarkBg = Color(0xFF1E1E1E)
private val DarkCard = Color(0xFF2D2D2D)
private val GreenAccent = Color(0xFF4EC9B0)
private val TextPrimary = Color(0xFFD4D4D4)
private val TextSecondary = Color(0xFF999999)
private val RedError = Color(0xFFF44747)
private val YellowWarn = Color(0xFFDCDCA4)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VideoCrawlerApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCrawlerApp() {
    val ctx = LocalContext.current
    val manager = remember { YtDlpManager(ctx) }
    val scope = rememberCoroutineScope()
    var ready by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf("") }
    var videoInfo by remember { mutableStateOf<VideoInfo?>(null) }
    var selectedFormat by remember { mutableStateOf("best") }
    var parsing by remember { mutableStateOf(false) }
    var loadingMsg by remember { mutableStateOf("初始化中...") }
    val tasks by manager.tasks.collectAsState()

    LaunchedEffect(Unit) {
        try {
            manager.ensureBinary()
            ready = true
        } catch (e: Exception) {
            loadingMsg = "下载yt-dlp失败: ${e.message}"
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = GreenAccent,
            background = DarkBg,
            surface = DarkCard,
            onPrimary = Color.Black,
            onBackground = TextPrimary,
            onSurface = TextPrimary,
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = DarkBg) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                // Title
                Text("Video Crawler", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = GreenAccent, modifier = Modifier.padding(bottom = 8.dp))

                if (!ready) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = GreenAccent)
                            Spacer(Modifier.height(12.dp))
                            Text(loadingMsg, color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                    return@Column
                }

                // URL input
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = url, onValueChange = { url = it },
                        modifier = Modifier.weight(1f).height(52.dp),
                        placeholder = { Text("粘贴视频链接", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenAccent,
                            unfocusedBorderColor = Color(0xFF3D3D3D),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = GreenAccent,
                        ),
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                parsing = true
                                videoInfo = manager.parseVideo(url)
                                parsing = false
                            }
                        },
                        enabled = !parsing && url.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                        modifier = Modifier.height(52.dp)
                    ) { Text(if (parsing) "解析中" else "解析", color = Color.Black) }
                }

                Text("支持: Bilibili, Twitter, TikTok, 抖音, 微博 等",
                    fontSize = 10.sp, color = RedError, modifier = Modifier.padding(top = 2.dp))

                // Preview
                videoInfo?.let { info ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                AsyncImage(
                                    model = ImageRequest.Builder(ctx)
                                        .data(info.thumbnail).crossfade(true).build(),
                                    contentDescription = null,
                                    modifier = Modifier.size(120.dp, 76.dp).clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(info.title, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                        color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text("上传者: ${info.uploader}", fontSize = 11.sp, color = TextSecondary)
                                    Text("时长: ${info.duration}", fontSize = 11.sp, color = TextSecondary)
                                    Text("分辨率: ${info.resolution}", fontSize = 11.sp, color = TextSecondary)
                                    Text("大小: ${info.size}", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                DropdownMenuSample(info.formats.map { it.label to it.id },
                                    onSelect = { selectedFormat = it })
                                Button(
                                    onClick = {
                                        manager.startDownload(url, selectedFormat, info.title)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenAccent)
                                ) { Text("下载", color = Color.Black) }
                            }
                        }
                    }
                }

                // Tasks
                if (tasks.isNotEmpty()) {
                    Text("下载任务", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        color = TextPrimary, modifier = Modifier.padding(vertical = 6.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(tasks) { task ->
                            TaskCard(task, manager)
                        }
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun DropdownMenuSample(options: List<Pair<String, String>>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(options.firstOrNull()?.second ?: "best") }
    Box {
        Button(
            onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D3D3D)),
            modifier = Modifier.height(40.dp)
        ) { Text(options.find { it.second == selected }?.first ?: "最佳画质",
                color = TextPrimary, fontSize = 13.sp) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (label, id) ->
                DropdownMenuItem(text = { Text(label, color = TextPrimary) },
                    onClick = { selected = id; onSelect(id); expanded = false })
            }
        }
    }
}

@Composable
fun TaskCard(task: DownloadTask, manager: YtDlpManager) {
    val statusColor = when (task.status) {
        DownloadStatus.DOWNLOADING -> GreenAccent
        DownloadStatus.PAUSED -> YellowWarn
        DownloadStatus.COMPLETED -> GreenAccent
        DownloadStatus.ERROR -> RedError
        DownloadStatus.CANCELLED -> TextSecondary
        DownloadStatus.PARSING -> GreenAccent
    }
    val statusText = when (task.status) {
        DownloadStatus.DOWNLOADING -> "下载中"
        DownloadStatus.PAUSED -> "已暂停"
        DownloadStatus.COMPLETED -> "已完成"
        DownloadStatus.ERROR -> "错误"
        DownloadStatus.CANCELLED -> "已取消"
        DownloadStatus.PARSING -> "解析中"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(task.title, modifier = Modifier.weight(1f), fontSize = 12.sp,
                    fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(statusText, fontSize = 11.sp, color = statusColor)
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { task.percent / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = if (task.status == DownloadStatus.PAUSED) YellowWarn else GreenAccent,
                trackColor = Color(0xFF1E1E1E)
            )
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("%.0f%%".format(task.percent), fontSize = 10.sp, color = TextSecondary)
                Text(task.speed, fontSize = 10.sp, color = TextSecondary)
                Text(task.eta, fontSize = 10.sp, color = TextSecondary)
            }
            if (task.error.isNotBlank()) {
                Text(task.error, fontSize = 10.sp, color = RedError)
            }
            if (task.status == DownloadStatus.DOWNLOADING || task.status == DownloadStatus.PAUSED) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (task.status == DownloadStatus.DOWNLOADING) {
                        SmallButton("暂停", YellowWarn) { manager.pauseTask(task.id) }
                    } else {
                        SmallButton("继续", GreenAccent) { manager.resumeTask(task.id) }
                    }
                    SmallButton("取消", RedError) { manager.cancelTask(task.id) }
                }
            }
        }
    }
}

@Composable
fun SmallButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = Modifier.height(28.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        shape = RoundedCornerShape(4.dp)
    ) { Text(text, fontSize = 11.sp, color = Color(0xFF1E1E1E)) }
}
