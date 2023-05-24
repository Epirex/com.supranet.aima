package com.supranet.doctormarketing

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var chatLinearLayout: LinearLayout
    private lateinit var chatScrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
        chatScrollView = findViewById(R.id.scrollView)
        chatLinearLayout = findViewById(R.id.chatLinearLayout)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        sendButton.setOnClickListener {
            val message = messageEditText.text.toString()
            if (message.isNotEmpty()) {
                sendMessageToChatGPT(message)
                messageEditText.text.clear()
            }
        }
        // Saludo inicial del bot
        addMessageToChatView("Hola! soy AIMA. Doctora Marketing, una inteligencia artificial desarrollada por Supranet. Preguntame lo que quieras :)", Gravity.START)
    }

    private fun sendMessageToChatGPT(message: String) {
        val client = OkHttpClient.Builder()
            .readTimeout(60000, TimeUnit.MILLISECONDS)
            .build()

        val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val json = JSONObject()
            .put("model", "gpt-3.5-turbo")
            .put(
                "messages", JSONArray()
                    .put(JSONObject().put("role", "system").put("content", "You"))
                    .put(JSONObject().put("role", "user").put("content", message))
            )

        val requestBody = json.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader(
                "Authorization",
                "Bearer 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"
            )
            .post(requestBody)
            .build()

        // mensaje usuario
        runOnUiThread {
            addMessageToChatView("$message", Gravity.END)
        }

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    val responseJson = JSONObject(responseBody.string())
                    Log.d("API Response", responseJson.toString())
                    if (responseJson.has("choices")) {
                        val choices = responseJson.getJSONArray("choices")
                        if (choices.length() > 0) {
                            val reply = choices.getJSONObject(0).getJSONObject("message")
                                .getString("content")
                            // mensaje bot
                            runOnUiThread {
                                addMessageToChatView("$reply", Gravity.START)
                                chatScrollView.post {
                                    chatScrollView.fullScroll(View.FOCUS_DOWN)
                                }
                            }
                        }
                    } else {
                        runOnUiThread {
                            addMessageToChatView("Error: Invalid response", Gravity.START)
                        }
                    }
                }
            }
        })
    }

    private fun addMessageToChatView(message: String, gravity: Int) {
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val textView = TextView(this)
        textView.text = message
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
        textView.setBackgroundResource(R.drawable.chat_bubble)
        textView.setPadding(16, 8, 16, 8)
        textView.layoutParams = layoutParams
        //textView.gravity = Gravity.CENTER_VERTICAL
        textView.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onPrimary))

        // Alinear texto en el layout
        if (gravity == Gravity.END) {
            textView.setBackgroundResource(R.drawable.chat_bubble_user)
            layoutParams.setMargins(150, 8, 16, 8)
        } else {
            textView.setBackgroundResource(R.drawable.chat_bubble_bot)
            layoutParams.setMargins(16, 8, 150, 8)
        }

        val parentLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val parentLinearLayout = LinearLayout(this)
        parentLinearLayout.layoutParams = parentLayoutParams

        if (gravity == Gravity.END) {
            parentLinearLayout.gravity = Gravity.END
        } else {
            parentLinearLayout.gravity = Gravity.START
        }

        parentLinearLayout.addView(textView)

        chatLinearLayout.addView(parentLinearLayout)
    }
}
