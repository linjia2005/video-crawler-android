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
import android.widget.Toast

class MainActivity : Activity() {
    private var webView: WebView? = null
    private lateinit var prefs: SharedPreferences
    private val defaultUrl = "http://192.168.1.100:5000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("config", MODE_PRIVATE)

        try {
            initWebView()
        } catch (e: Exception) {
            Toast.makeText(this, "WebView初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
        }

        findViewById<Button>(R.id.btnSettings).setOnClickListener { showSettings() }
    }

    private fun initWebView() {
        val wv = findViewById<WebView>(R.id.webView)
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            setSupportZoom(false)
        }
        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, url: String): Boolean {
                v.loadUrl(url)
                return true
            }
        }
        val url = prefs.getString("server_url", defaultUrl) ?: defaultUrl
        wv.loadUrl(url)
        webView = wv
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
                findViewById<WebView>(R.id.webView).loadUrl(url)
            }
            setNegativeButton("取消", null)
        }.show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView?.canGoBack() == true) {
            webView?.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
