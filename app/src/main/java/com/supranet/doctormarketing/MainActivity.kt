package com.supranet.doctormarketing

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
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
import java.util.*
import javax.activation.CommandMap
import javax.activation.MailcapCommandMap
import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

class MainActivity : AppCompatActivity() {

    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var chatLinearLayout: LinearLayout
    private lateinit var chatScrollView: ScrollView
    private val messageHistory: MutableList<String> = mutableListOf()
    private val messageHistoryToSend: MutableList<Pair<String, String>> = mutableListOf()
    private var isBotTyping: Boolean = false
    private val messageIntial = "Mi nombre es AIMA, soy una Inteligencia Artificial especializada en marketing, puedes realizarme consultas para ayudarte en tu emprendimiento ¿En que te puedo ayudar?"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
        chatScrollView = findViewById(R.id.scrollView)
        chatLinearLayout = findViewById(R.id.chatLinearLayout)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        val clearButton: ImageButton = findViewById(R.id.clearButton)
        val homeButton: ImageButton =findViewById(R.id.homeButton)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Configurar teclado fisico
        messageEditText.requestFocus()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        messageEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event != null && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)
            ) {
                val message = messageEditText.text.toString()
                if (message.isNotEmpty()) {
                    sendMessageToChatGPT(message)
                    messageEditText.text.clear()
                    hideKeyboard()
                    messageEditText.requestFocus()
                }
                true
            } else {
                false
            }
        }

        messageEditText.setOnKeyListener{ _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_F5 -> {
                        clearChat()
                        messageEditText.requestFocus()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        scrollChatUp()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        scrollChatDown()
                        true
                    }
                }
            }
            false
        }

        // boton de enviar
        sendButton.setOnClickListener {
            val message = messageEditText.text.toString()
            if (message.isNotEmpty()) {
                sendMessageToChatGPT(message)
                messageEditText.text.clear()
                hideKeyboard()
            }
        }

        // boton de limpieza
        clearButton.setOnClickListener {
            clearChat()
        }

        // Boton home
        homeButton.setOnClickListener{
            val intent = Intent(this, StartActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Mantener pantalla siempre encendida
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Saludo inicial del bot
        addMessageToChatView(messageIntial, Gravity.START)
    }

    private fun sendMessageToChatGPT(message: String) {
        // Agregar el mensaje actual al historial
        messageHistory.add(message)
        messageHistoryToSend.add(Pair("Usted", message))

        // Mantener solo los últimos 10 mensajes en el historial
        if (messageHistory.size > 10) {
            messageHistory.removeAt(0)
        }

        val tipoEmprendimiento = intent.getStringExtra("storedInformation")
        Log.d("MainActivity", "storedInformation in private : $tipoEmprendimiento")

        val promptDefault = "Te llamas AIMA y sos una asistente de marketing. " +
                "Solo responderás preguntas relacionadas al marketing y los negocios. " +
                "Al final de cada mensaje harás una pregunta relacionada al tema para que la conversación fluya mejor. " +
                "El usuario tendrá un emprendimiento de: $tipoEmprendimiento"

        val messagesArray = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", promptDefault))
            val lastMessages = messageHistoryToSend.takeLast(5)
            lastMessages.forEach {
                put(JSONObject().put("role", "user").put("content", it.second))
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
            scrollChat()
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
                            // Guardar mensaje del bot en el historial
                            messageHistoryToSend.add(Pair("AIMA", reply))
                            // mensaje bot
                            runOnUiThread {
                                hideBotTyping()
                                addMessageToChatView("$reply", Gravity.START)
                                chatScrollView.post {
                                    scrollChat()
                                    messageEditText.requestFocus()
                                }
                            }
                        }
                    } else {
                        runOnUiThread {
                            addMessageToChatView("Error: Ups! vuelve a intentarlo.", Gravity.START)
                            messageEditText.requestFocus()
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
        textView.setPadding(32, 16, 32, 16)
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

    // Enviar historial del chat en segundo plano
    private inner class SendEmailTask(private val email: String, private val conversation: String) : AsyncTask<Void, Void, Boolean>() {
        override fun doInBackground(vararg params: Void): Boolean {
            try {
                val properties = Properties().apply {
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                }

                val session = Session.getInstance(properties, object : javax.mail.Authenticator() {
                    override fun getPasswordAuthentication(): javax.mail.PasswordAuthentication {
                        return javax.mail.PasswordAuthentication("supranet.logos@gmail.com", "npmtportarqirmyk")
                    }
                })

                val message = MimeMessage(session)
                message.setFrom(InternetAddress("supranet.logos@gmail.com"))
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email))
                message.subject = "Supranet: ¡Esta fue tu conversacion con AIMA!"
                message.sentDate = Date()

                // No me pregunten que es esto, lo saque de stackoverflow y me permitio enviar mails
                val mc = CommandMap.getDefaultCommandMap() as MailcapCommandMap
                mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html")
                mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml")
                mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain")
                mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed")
                mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822")
                CommandMap.setDefaultCommandMap(mc)

                val textPart = MimeBodyPart().apply {
                    setText(conversation)
                }

                val multipart = MimeMultipart().apply {
                    addBodyPart(textPart)
                }
                message.setContent(multipart)

                val transport = session.transport
                transport.connect()
                transport.sendMessage(message, message.allRecipients)
                transport.close()

                return true
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }

        override fun onPostExecute(result: Boolean) {
            if (result) {
                Toast.makeText(applicationContext, "Correo enviado correctamente", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext, "Error al enviar el correo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendEmailInBackground(email: String, conversation: String) {
        val sendEmailTask = SendEmailTask(email, conversation)
        sendEmailTask.execute()
    }


    private fun clearChat() {
        val conversation = buildConversation()

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Enviar conversación por correo")
        alertDialogBuilder.setMessage("Por favor, ingresa tu correo electrónico:")
        val inputEmail = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        inputEmail.layoutParams = lp
        alertDialogBuilder.setView(inputEmail)
        alertDialogBuilder.setPositiveButton("Enviar") { dialog, _ ->
            val email = inputEmail.text.toString()
            sendEmailInBackground(email, conversation)
            chatLinearLayout.removeAllViews()
            messageHistory.clear()
            addMessageToChatView(messageIntial, Gravity.START)
            dialog.dismiss()
            val intent = Intent(this, StartActivity::class.java)
            startActivity(intent)
        }
        alertDialogBuilder.setNegativeButton("Cancelar") { dialog, _ ->
            chatLinearLayout.removeAllViews()
            messageHistory.clear()
            addMessageToChatView(messageIntial, Gravity.START)
            dialog.dismiss()
        }
        alertDialogBuilder.create().show()
    }

    private fun buildConversation(): String {
        val conversation = StringBuilder()
        val additionalMessage = "Espero que te encuentres muy bien, te comparto una copia de nuestra conversacion. Muchas gracias por participar :)"
        conversation.append("$additionalMessage\n\n")
        for (pair in messageHistoryToSend) {
            val role = pair.first
            val message = pair.second
            conversation.append("$role: $message\n")
        }
        return conversation.toString()
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
                chatLinearLayout.removeViewAt(chatLinearLayout.childCount - 1)
            }
        }
    }
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(messageEditText.windowToken, 0)
    }
    private fun scrollChatUp() {
        chatScrollView.post {
            val scrollAmount = 200 // Cantidad de desplazamiento en píxeles
            chatScrollView.scrollBy(0, -scrollAmount)
        }
    }
    private fun scrollChatDown() {
        chatScrollView.post {
            val scrollAmount = 200 // Cantidad de desplazamiento en píxeles
            chatScrollView.scrollBy(0, scrollAmount)
        }
    }
    private fun scrollChat() {
        chatScrollView.postDelayed({
            chatScrollView.fullScroll(View.FOCUS_DOWN)
            messageEditText.requestFocus()
        }, 100)
    }
}
