package com.videocrawler

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var prefs: SharedPreferences
    private val defaultUrl = "http://192.168.1.100:5000"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("config", MODE_PRIVATE)

        webView = findViewById(R.id.webView)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            setSupportZoom(false)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, url: String): Boolean {
                v.loadUrl(url)
                return true
            }
        }

        val url = prefs.getString("server_url", defaultUrl) ?: defaultUrl
        webView.loadUrl(url)

        findViewById<Button>(R.id.btnSettings).setOnClickListener { showSettings() }
    }

    private fun showSettings() {
        val input = EditText(this).apply {
            setText(prefs.getString("server_url", defaultUrl))
            hint = "http://192.168.1.100:5000"
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
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
