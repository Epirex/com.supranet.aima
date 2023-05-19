package com.supranet.doctormarketing

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var chatTextView: TextView

    private fun testAPIKey() {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://api.openai.com/v1/engines")
            .addHeader("Authorization", "Bearer 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") // Reemplaza YOUR_API_KEY con tu clave de la API
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Manejar la falla de la solicitud
                e.printStackTrace()
                runOnUiThread {
                    chatTextView.append("API Key is invalid\n")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // La clave de API es válida
                    runOnUiThread {
                        chatTextView.append("API Key is valid\n")
                    }
                } else {
                    // La clave de API es inválida
                    runOnUiThread {
                        chatTextView.append("API Key is invalid\n")
                    }
                }
            }
        })
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
        chatTextView = findViewById(R.id.chatTextView)

        sendButton.setOnClickListener {
            val message = messageEditText.text.toString()
            if (message.isNotEmpty()) {
                testAPIKey()
                sendMessageToChatGPT(message)
            }
        }
    }

    private fun sendMessageToChatGPT(message: String) {
        val client = OkHttpClient()

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
                            val reply = choices.getJSONObject(0).getString("message")
                            runOnUiThread {
                                chatTextView.append("Bot: $reply\n")
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