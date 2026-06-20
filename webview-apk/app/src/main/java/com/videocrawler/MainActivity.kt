package com.videocrawler

import android.app.Activity
import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var prefs: SharedPreferences
    private val defaultUrl = "http://192.168.1.100:5000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
