package com.projects.asgrebennikov.repetitor

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button

class BookSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_selection)

        val killButton = findViewById<View>(R.id.killButton) as Button
        killButton.setOnClickListener {
            val intent = Intent(this@BookSelectionActivity, SentenceActivity::class.java)
            intent.putExtra("eng_text_name", "english_text")
            intent.putExtra("rus_text_name", "russian_text")
            startActivity(intent)
        }

        val crimeButton = findViewById<View>(R.id.crimeButton) as Button
        crimeButton.setOnClickListener {
            val intent = Intent(this@BookSelectionActivity, SentenceActivity::class.java)
            intent.putExtra("eng_text_name", "crime_english_text")
            intent.putExtra("rus_text_name", "crime_russian_text")
            startActivity(intent)
        }
    }
}
