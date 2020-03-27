package com.projects.asgrebennikov.repetitor

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import java.util.*
import javax.mail.Authenticator
import javax.mail.Message.RecipientType
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

fun sendEmail(user: String, tos: Array<String>, ccs: Array<String>, title: String,
              body: String, password: String) {
    val props = Properties()
    val host = "smtp.mail.ru"
    with (props) {
        put("mail.smtp.host", host)
        put("mail.smtp.port", "587") // for TLS
        put("mail.smtp.auth", "true")
        put("mail.smtp.starttls.enable", "true")
    }
    val auth = object: Authenticator() {
        protected override fun getPasswordAuthentication() =
                PasswordAuthentication(user, password)
    }
    val session = Session.getInstance(props, auth)
    val message = MimeMessage(session)
    message.setFrom(InternetAddress(user))
    for (to in tos) message.addRecipient(RecipientType.TO, InternetAddress(to))
    for (cc in ccs) message.addRecipient(RecipientType.TO, InternetAddress(cc))
    message.setSubject(title)
    message.setText(body)
    val transport = session.getTransport("smtp")
    with (transport) {
        connect(host, user, password)
        sendMessage(message, message.allRecipients)
        close()
    }
}

class GetFeedbackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_feedback)

        val sendFeedbackButton = findViewById<View>(R.id.sendFeedbackButton) as Button
        val feedbackTextEdit = findViewById<View>(R.id.feedback_text) as EditText

        sendFeedbackButton.setOnClickListener {
            Flowable.fromCallable({}).subscribeOn(Schedulers.io()).subscribe({
                val user = "repetitor.feedback@list.ru"
                val tos = arrayOf<String>("alexex111@yandex.ru")
                val ccs = arrayOf<String>()
                val title = "repetitor feedback"
                val body = feedbackTextEdit.text.toString()
                val password = "fucking.strong.password.blyat"
                finish()
                sendEmail(user, tos, ccs, title, body, password)
            })
        }
    }
}
