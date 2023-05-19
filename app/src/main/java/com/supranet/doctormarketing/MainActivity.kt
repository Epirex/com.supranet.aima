package com.supranet.doctormarketing

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var chatTextView: TextView
    private lateinit var chatScrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
        chatScrollView = findViewById(R.id.scrollView)
        chatTextView = findViewById(R.id.chatTextView)

        chatTextView.textSize = 20f // Establecer tamaño de letra en 18 píxeles

        sendButton.setOnClickListener {
            val message = messageEditText.text.toString()
            if (message.isNotEmpty()) {
                sendMessageToChatGPT(message)
                messageEditText.text.clear()
            }
        }
    }

    private fun sendMessageToChatGPT(message: String) {
        val client = OkHttpClient.Builder()
            .callTimeout(120, TimeUnit.SECONDS)
            .build()

        val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val json = JSONObject()
            .put("model", "gpt-3.5-turbo")
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", "You"))
                .put(JSONObject().put("role", "user").put("content", message))
            )

        val requestBody = json.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer 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")
            .post(requestBody)
            .build()

        // Agregar tu mensaje al TextView
        runOnUiThread {
            chatTextView.append("Usuario: $message\n")
        }

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Manejar la falla de la solicitud
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    val responseJson = JSONObject(responseBody.string())
                    Log.d("API Response", responseJson.toString())
                    if (responseJson.has("choices")) {
                        val choices = responseJson.getJSONArray("choices")
                        if (choices.length() > 0) {
                            val reply = choices.getJSONObject(0).getJSONObject("message").getString("content")
                            runOnUiThread {
                                chatTextView.append("Doctor: $reply\n")
                                chatScrollView.post {
                                    chatScrollView.fullScroll(View.FOCUS_DOWN)
                                }
                            }
                        }
                    } else {
                        // No se encontró la clave "choices" en la respuesta JSON
                        runOnUiThread {
                            chatTextView.append("Error: Invalid response\n")
                        }
                    }
                }
            }
        })
    }
}