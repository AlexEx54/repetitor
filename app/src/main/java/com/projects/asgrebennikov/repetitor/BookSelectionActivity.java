package com.projects.asgrebennikov.repetitor;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class BookSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_selection);

        Button killButton = (Button) findViewById(R.id.killButton);
        killButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public synchronized void onClick(View v) {
                Intent intent = new Intent(BookSelectionActivity.this, SentenceActivity.class);
                intent.putExtra("eng_text_name","english_text");
                intent.putExtra("rus_text_name","russian_text");
                startActivity(intent);
            }
        });

        Button crimeButton = (Button) findViewById(R.id.crimeButton);
        crimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public synchronized void onClick(View v) {
                Intent intent = new Intent(BookSelectionActivity.this, SentenceActivity.class);
                intent.putExtra("eng_text_name","crime_english_text");
                intent.putExtra("rus_text_name","crime_russian_text");
                startActivity(intent);
            }
        });

        Button scarletButton = (Button) findViewById(R.id.scarletButton);
        scarletButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public synchronized void onClick(View v) {
                Intent intent = new Intent(BookSelectionActivity.this, SentenceActivity.class);
                intent.putExtra("eng_text_name","study_in_scarlet_eng");
                intent.putExtra("rus_text_name","study_in_scarlet_rus");
                startActivity(intent);
            }
        });

        Button firTreeButton = (Button) findViewById(R.id.firTreeButton);
        firTreeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public synchronized void onClick(View v) {
                Intent intent = new Intent(BookSelectionActivity.this, SentenceActivity.class);
                intent.putExtra("eng_text_name","fir_tree_eng");
                intent.putExtra("rus_text_name","fir_tree_rus");
                startActivity(intent);
            }
        });
    }
}
