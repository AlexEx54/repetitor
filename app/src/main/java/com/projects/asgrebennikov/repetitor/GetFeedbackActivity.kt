package com.projects.asgrebennikov.repetitor

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_get_feedback.*

import java.util.Properties
import javax.mail.Authenticator
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.internet.MimeMessage
import javax.mail.internet.InternetAddress
import javax.mail.Message.RecipientType
import javax.mail.Transport

fun sendEmail(user: String, tos: Array<String>, ccs: Array<String>, title: String,
              body: String, password: String) {
    val props = Properties()
    val host = "smtp.gmail.com"
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
    with (message) {
        setFrom(InternetAddress(user))
        for (to in tos) addRecipient(RecipientType.TO, InternetAddress(to))
        for (cc in ccs) addRecipient(RecipientType.TO, InternetAddress(cc))
        setSubject(title)
        setText(body)
    }
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
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        val user = "some.user@gmail.com"
        val tos = arrayOf("other.user@otherserver.com")
        val ccs = arrayOf<String>()
        val title = "Rosetta Code Example"
        val body = "This is just a test email"
        val password = "secret"
        sendEmail(user, tos, ccs, title, body, password)
    }

}
