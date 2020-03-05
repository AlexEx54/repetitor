package com.projects.asgrebennikov.repetitor;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.google.android.gms.ads.MobileAds;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button practiceButton = (Button) findViewById(R.id.startPracticeButton);
        practiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public synchronized void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BookSelectionActivity.class);
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

        Button feedbackButton = (Button) findViewById(R.id.writeFeedbackButton);
        feedbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public synchronized void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GetFeedbackActivity.class);
                startActivity(intent);
            }
        });

        //MobileAds.initialize(this, "ca-app-pub-2381885173378825~5894393156");
    }
}
