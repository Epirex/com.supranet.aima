package com.supranet.doctormarketing

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class StartActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var editText: EditText
    private lateinit var botonIniciar: Button
    private var storedInformation: String = ""

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        webView = findViewById(R.id.webView)
        editText = findViewById(R.id.editText)
        botonIniciar = findViewById(R.id.iniciar)

        // Configuración del WebView
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = WebViewClient()
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true

        // Mantener pantalla siempre encendida
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        //webView.loadUrl("file:///android_asset/index.html")
        webView.loadUrl("http://www.poster.com.ar/aima")

        botonIniciar.setOnClickListener {
            val inputText = editText.text.toString()
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("storedInformation", inputText)
            startActivity(intent)
        }
    }
}