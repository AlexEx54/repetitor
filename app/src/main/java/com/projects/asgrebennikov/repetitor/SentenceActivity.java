package com.projects.asgrebennikov.repetitor;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.Guideline;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Vector;

import backend.Database;
import backend.DatabaseImpl;
import backend.Sentence;
import backend.TextSupplier;
import backend.TextSupplierImpl;
import backend.Vocabulary;
import backend.Word;
import backend.WordContext;
import backend.YandexVocabularyImpl;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


public class SentenceActivity extends AppCompatActivity {

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sentence);
        wordsList_ = new ArrayList<WordListItem>();

        SetWordListViewHandlers();
        SetNextButtonHandlers();
        SetTextViewHandlers();

        try {
            InputStream rus_stream = getResources().openRawResource(
                    getResources().getIdentifier("russian_text",
                            "raw", getPackageName()));
            InputStream eng_stream = getResources().openRawResource(
                    getResources().getIdentifier("english_text",
                            "raw", getPackageName()));

            rusTextSupplier_ = new TextSupplierImpl(getFilesDir().getAbsolutePath(), rus_stream, "russian_text");
            engTextSupplier_ = new TextSupplierImpl(getFilesDir().getAbsolutePath(), eng_stream, "english_text");

//            rusTextSupplier_.SaveCursor();
//            engTextSupplier_.SaveCursor();

            rusTextSupplier_.LoadCursor();
            engTextSupplier_.LoadCursor();

            rusSentence_ = rusTextSupplier_.GetNextSentence();
            engSentence_ = engTextSupplier_.GetNextSentence();

            currentDirection_ = Vocabulary.TranslateDirection.RU_EN;

            TextView textView = (TextView) findViewById(R.id.sentenceTextView);
            textView.setText(rusSentence_.AsString());
            Vector<Word> words = rusSentence_.GetWords();
            wordsList_.addAll(ToWordListItems(words, Vocabulary.TranslateDirection.RU_EN));

            db_ = new DatabaseImpl();
            db_.Open(getFilesDir().toString(),"1.0.0");
        }
        catch (Exception e) {
            finish();
        }
    }


    private Vector<WordListItem> ToWordListItems(Vector<Word> words,
                                                 Vocabulary.TranslateDirection translateDirection) {
        Vector<WordListItem> result = new Vector<WordListItem>();

        for (Word word: words) {
            result.add(new WordListItem(word, translateDirection));
        }

        return result;
    }


    private void SetWordListViewHandlers() {
        ListView lv = (ListView) findViewById(R.id.wordsListView);
        ArrayAdapter<WordListItem> adapter = new WordsArrayAdapter<WordListItem>(this,
                android.R.layout.simple_list_item_1,
                wordsList_);
        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {

                WordListItem item = (WordListItem) parent.getItemAtPosition(position);
                if (item.getTranslations() != null) {
                    item.setFolded(!item.isFolded());
                    adapter.notifyDataSetChanged();
                    return;
                }

                Flowable.fromCallable(() -> {
                    YandexVocabularyImpl vocabulary = new YandexVocabularyImpl();
                    Vector<Word> translations = vocabulary.Translate(item.getWord(), item.getTranslateDirection());
                    return translations;
                })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe((Vector<Word> words) -> {

                            item.setTranslations(words);
                            item.setFolded(!item.isFolded());
                            adapter.notifyDataSetChanged();
                            SaveWordToDb(item.getWord(), item.getTranslateDirection());

                        }, Throwable::printStackTrace);
            }


            private void SaveWordToDb(Word word, Vocabulary.TranslateDirection translateDirection) {
                WordContext wordContext = new WordContext();
                wordContext.timestamp = System.currentTimeMillis();
                wordContext.word = word;
                wordContext.translateDirection = translateDirection;

                if (currentDirection_ == Vocabulary.TranslateDirection.RU_EN) {
                    wordContext.containingSentence = rusSentence_;
                    wordContext.complementarySentence = engSentence_;
                } else {
                    wordContext.containingSentence = engSentence_;
                    wordContext.complementarySentence = rusSentence_;
                }

                db_.SaveWord(wordContext);
            }
        });
    }


    private void SetNextButtonHandlers() {
        Button nextSentenceButton = (Button) findViewById(R.id.nextButton);
        nextSentenceButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public synchronized void onClick(View v) {
                Sentence sentence = null;

                if (currentDirection_ == Vocabulary.TranslateDirection.RU_EN) {
                    currentDirection_ = Vocabulary.TranslateDirection.EN_RU;
                    sentence = engSentence_;
                } else {
                    engSentence_ = engTextSupplier_.GetNextSentence();
                    rusSentence_ = rusTextSupplier_.GetNextSentence();
                    currentDirection_ = Vocabulary.TranslateDirection.RU_EN;
                    sentence = rusSentence_;
                }

                rusTextSupplier_.SaveCursor();
                engTextSupplier_.SaveCursor();

                TextView textView = (TextView) findViewById(R.id.sentenceTextView);
                ListView lv = (ListView) findViewById(R.id.wordsListView);

                textView.setText(sentence.AsString());
                Vector<Word> words = sentence.GetWords();
                ArrayAdapter<Word> adapter = (ArrayAdapter<Word>) lv.getAdapter();
                wordsList_.clear();
                wordsList_.addAll(ToWordListItems(words, currentDirection_));
                adapter.notifyDataSetChanged();
            }
        });
    }


    private void SetTextViewHandlers() {
        TextView textView = (TextView) findViewById(R.id.sentenceTextView);
        textView.setGravity(Gravity.CENTER);

        ListView lv = (ListView) findViewById(R.id.wordsListView);

        textView.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeLeft() {
                TextSupplier currentSupplier = null;


                switch (currentDirection_) {
                    case RU_EN: {
                        currentSupplier = rusTextSupplier_;
                        break;
                    }
                    case EN_RU: {
                        currentSupplier = engTextSupplier_;
                        break;
                    }
                }

                Sentence currentSentence = null;
                currentSentence = currentSupplier.GetNextSentence();
                currentSupplier.SaveCursor();

                textView.setText(currentSentence.AsString());
                ArrayAdapter<Word> adapter = (ArrayAdapter<Word>) lv.getAdapter();
                wordsList_.clear();
                wordsList_.addAll(ToWordListItems(currentSentence.GetWords(), currentDirection_));
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onSwipeRight() {
                TextSupplier currentSupplier = null;

                switch (currentDirection_) {
                    case RU_EN: {
                        currentSupplier = rusTextSupplier_;
                        break;
                    }
                    case EN_RU: {
                        currentSupplier = engTextSupplier_;
                        break;
                    }
                }

                Sentence currentSentence = null;
                currentSentence = currentSupplier.GetPrevSentence();
                currentSupplier.SaveCursor();

                textView.setText(currentSentence.AsString());
                ArrayAdapter<Word> adapter = (ArrayAdapter<Word>) lv.getAdapter();
                wordsList_.clear();
                wordsList_.addAll(ToWordListItems(currentSentence.GetWords(), currentDirection_));
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onSwipeBottom() {
                Guideline guideLine = (Guideline) findViewById(R.id.guideline2);
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideLine.getLayoutParams();
                params.guidePercent = 0.5f;
                guideLine.setLayoutParams(params);
            }

            @Override
            public void onSwipeTop() {
                Guideline guideLine = (Guideline) findViewById(R.id.guideline2);
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideLine.getLayoutParams();
                params.guidePercent = 0.2f;
                guideLine.setLayoutParams(params);
            }

        });
    }


    private void PassTextToGTranslateApp() {
        //                Intent intent = new Intent();
//                intent.setAction(Intent.ACTION_VIEW);
//                intent.setPackage("com.google.android.apps.translate");
//
//                Uri uri = new Uri.Builder()
//                        .scheme("http")
//                        .authority("translate.google.com")
//                        .path("/m/translate")
//                        .appendQueryParameter("q", "корова")
//                        .appendQueryParameter("tl", "en") // target language
//                        .appendQueryParameter("sl", "ru") // source language
//                        .build();
//                //intent.setType("text/plain"); //not needed, but possible
//                intent.setData(uri);
//                startActivity(intent);
    }


    private TextSupplier rusTextSupplier_;
    private TextSupplier engTextSupplier_;
    private ArrayList<WordListItem> wordsList_;

    private Vocabulary.TranslateDirection currentDirection_;
    private Sentence rusSentence_;
    private Sentence engSentence_;
    private Database db_;

} // class SentenceActivity
