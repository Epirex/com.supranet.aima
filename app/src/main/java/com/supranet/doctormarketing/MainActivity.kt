package com.supranet.doctormarketing

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
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
    private val messageHistory: MutableList<String> = mutableListOf()
    private var isBotTyping: Boolean = false

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

        // boton de enviar
        sendButton.setOnClickListener {
            val message = messageEditText.text.toString()
            if (message.isNotEmpty()) {
                sendMessageToChatGPT(message)
                messageEditText.text.clear()
            }
        }

        // boton de limpieza
        val clearButton: ImageButton = findViewById(R.id.clearButton)
        clearButton.setOnClickListener {
            clearChat()
        }

        // Mantener pantalla siempre encendida
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Saludo inicial del bot
        addMessageToChatView("Hola! soy AIMA. Una inteligencia artificial desarrollada por Supranet. Puedes realizarme consultas sobre marketing para ayudarte con tu emprendimiento.", Gravity.START)
    }

    private fun sendMessageToChatGPT(message: String) {
        // Agregar el mensaje actual al historial
        messageHistory.add(message)

        // Mantener solo los últimos 10 mensajes en el historial
        if (messageHistory.size > 10) {
            messageHistory.removeAt(0)
        }

        val messagesArray = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", "Eres una inteligencia artificial desarrollada por Supranet, te llamas AIMA, ahora eres una especialista en marketing con más de 20 años de experiencia. Tu objetivo será responder solamente a consultas relacionadas al marketing y los negocios. Responderás de forma amable y cortés, y con cada respuesta realizarás preguntas sobre la temática sobre la que estás charlando, buscando ayudar al usuario a descubrir otra información relevante que debe considerar sobre el tema en cuestión. Si te preguntan sobre temas que no tengan que ver con los negocios o el marketing, siempre darás por entendido que lo que se busca es una orientación de marketing respecto al texto introducido, y si hace falta más información para dar una respuesta asertiva, harás preguntas sobre el tema en cuestión solo si es absolutamente relevante."))
            for (i in messageHistory.indices) {
                put(JSONObject().put("role", "user").put("content", messageHistory[i]))
            }
        }

        val client = OkHttpClient.Builder()
            .readTimeout(60000, TimeUnit.MILLISECONDS)
            .build()

        val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val json = JSONObject()
            .put("model", "gpt-3.5-turbo")
            .put("messages", messagesArray)

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
            showBotTyping()
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
                                hideBotTyping()
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

    private fun clearChat() {
        chatLinearLayout.removeAllViews()
        messageHistory.clear()
        addMessageToChatView("Hola! soy AIMA. Una inteligencia artificial desarrollada por Supranet. Puedes realizarme consultas sobre marketing para ayudarte con tu emprendimiento.", Gravity.START)
    }
    private fun showBotTyping() {
        if (!isBotTyping) {
            isBotTyping = true
            val typingMessage = "Escribiendo..."
            runOnUiThread {
                addMessageToChatView(typingMessage, Gravity.START)
            }
        }
    }

    private fun hideBotTyping() {
        if (isBotTyping) {
            isBotTyping = false
            runOnUiThread {
                chatLinearLayout.removeViewAt(chatLinearLayout.childCount - 1) // Remover el último mensaje
            }
        }
    }
}
