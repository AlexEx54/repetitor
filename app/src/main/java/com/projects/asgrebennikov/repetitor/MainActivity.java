package com.projects.asgrebennikov.repetitor;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button practiceButton = (Button) findViewById(R.id.startPracticeButton);
        practiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public synchronized void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SentenceActivity.class);
                startActivity(intent);
            }
        });

        Button learningButton = (Button) findViewById(R.id.startLearningButton);
        learningButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public synchronized void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LearningWordsActivity.class);
                startActivity(intent);
            }
        });
    }
}
