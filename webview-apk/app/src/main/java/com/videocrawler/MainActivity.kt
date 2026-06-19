package com.videocrawler

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var prefs: SharedPreferences
    private val defaultUrl = "http://192.168.1.100:5000"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("config", MODE_PRIVATE)

        progressBar = findViewById(R.id.progressBar)
        webView = findViewById(R.id.webView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setColorSchemeColors(0xFF4EC9B0.toInt())
        swipeRefresh.setOnRefreshListener { webView.reload() }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            setSupportZoom(false)
            userAgentString = "Mozilla/5.0 (Linux; Android 10)"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                v.loadUrl(r.url.toString()); return true
            }
            override fun onPageStarted(v: WebView, url: String, favicon: Bitmap?) {
                progressBar.visibility = android.view.View.VISIBLE
            }
            override fun onPageFinished(v: WebView, url: String) {
                progressBar.visibility = android.view.View.GONE
                swipeRefresh.isRefreshing = false
                title = v.title
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(v: WebView, p: Int) { progressBar.progress = p }
        }

        webView.loadUrl(prefs.getString("server_url", defaultUrl) ?: defaultUrl)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "设置服务器").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) { showSettingsDialog(); return true }
        return super.onOptionsItemSelected(item)
    }

    private fun showSettingsDialog() {
        val input = EditText(this).apply {
            setText(prefs.getString("server_url", defaultUrl))
            hint = "http://192.168.1.100:5000"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
            selectAll()
        }
        AlertDialog.Builder(this).apply {
            setTitle("服务器地址")
            setView(input)
            setPositiveButton("连接") { _, _ ->
                val url = input.text.toString().trim()
                prefs.edit().putString("server_url", url).apply()
                webView.loadUrl(url)
            }
            setNegativeButton("取消", null)
        }.show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack(); return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
