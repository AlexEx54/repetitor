package com.projects.asgrebennikov.repetitor

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val practiceButton = findViewById<View>(R.id.startPracticeButton) as Button
        practiceButton.setOnClickListener {
            val intent = Intent(this@MainActivity, BookSelectionActivity::class.java)
            startActivity(intent)
        }

        val learningButton = findViewById<View>(R.id.startLearningButton) as Button
        learningButton.setOnClickListener {
            val intent = Intent(this@MainActivity, LearningWordsActivity::class.java)
            startActivity(intent)
        }
    }
}
