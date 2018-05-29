package com.projects.asgrebennikov.repetitor;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;

import backend.Database;
import backend.DatabaseImpl;

public class LearningWordsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.learning_words);

        db_ = new DatabaseImpl();
        db_.Open(getFilesDir().toString(),"1.0.0");


    }

    private ArrayList<WordListItem> learningWord_;
    private Database db_;
}
