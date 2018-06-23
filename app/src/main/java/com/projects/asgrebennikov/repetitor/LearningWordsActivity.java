package com.projects.asgrebennikov.repetitor;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Vector;

import backend.Database;
import backend.DatabaseImpl;
import backend.Word;
import backend.WordContext;
import backend.YandexVocabularyImpl;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class LearningWordsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.learning_words);

        db_ = new DatabaseImpl();
        db_.Open(getFilesDir().toString(),"1.0.0");

        learningWord_ = new ArrayList<WordListItem>();
        ListView learningWordsList = (ListView) findViewById(R.id.lw_learningWordList);
        listAdapter_ = new WordsArrayAdapter<WordListItem>(this,
                                                            android.R.layout.simple_list_item_1,
                                                            learningWord_);
        learningWordsList.setAdapter(listAdapter_);

        SetListViewClickHandler(learningWordsList);
        SetComplementarySentenceClickHandler();
        SetNextWordClickHandler();
        SetCompletedWordButtonHandler();
        SetTermsTextViewHandlers();

        ShowNextWordToLearn(null);
    }


    private void ShowNextWordToLearn(WordContext prevWord) {
        WordContext wordToLearn = db_.GetNextWord(prevWord);

        if ((wordToLearn == null) ||
            (wordToLearn.containingSentence == null) ||
            (wordToLearn.word == null)) {

            SetNothingToLearnView();
            return;
        }

        TextView containigTextView = (TextView) findViewById(R.id.containingSentenceTextView);
        containigTextView.setText(wordToLearn.containingSentence.AsString());

        TextView complementaryTextView = (TextView) findViewById(R.id.complementarySentenceTextView);
        complementaryTextView.setText("- - -");

        WordListItem listItem = new WordListItem(wordToLearn.word, wordToLearn.translateDirection);
        learningWord_.clear();
        learningWord_.add(listItem);
        listAdapter_.notifyDataSetChanged();

        currentWord_ = wordToLearn;
    }


    private void SetNothingToLearnView() {
        TextView containigTextView = (TextView) findViewById(R.id.containingSentenceTextView);
        TextView complementaryTextView = (TextView) findViewById(R.id.complementarySentenceTextView);

        containigTextView.setText("Nothing to learn :)");
        complementaryTextView.setText("- - -");
        learningWord_.clear();
        listAdapter_.notifyDataSetChanged();
    }


    private void SetListViewClickHandler(ListView listView) {

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                WordListItem item = (WordListItem) parent.getItemAtPosition(position);
                if (item.getTranslations() != null) {
                    item.setFolded(!item.isFolded());
                    listAdapter_.notifyDataSetChanged();
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
                            listAdapter_.notifyDataSetChanged();
                        }, Throwable::printStackTrace);
            }
        });
    }


    private void SetComplementarySentenceClickHandler() {
        TextView textView = (TextView) findViewById(R.id.complementarySentenceTextView);

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public synchronized void onClick(View v) {
                if ((currentWord_ == null) ||
                    (currentWord_.complementarySentence == null)) {

                    return;
                }

                textView.setText(currentWord_.complementarySentence.AsString());
            }
        });
    }


    private void SetNextWordClickHandler() {
        Button nextWordButton = (Button) findViewById(R.id.nextWordButton);

        nextWordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public synchronized void onClick(View v) {
                ShowNextWordToLearn(currentWord_);
            }
        });
    }


    private void SetCompletedWordButtonHandler() {
        Button button = (Button) findViewById(R.id.completedWordButton);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public synchronized void onClick(View v) {
                if (currentWord_ == null) {
                    return;
                }

                db_.RemoveWord(currentWord_);
                ShowNextWordToLearn(currentWord_);
            }
        });
    }


    private void SetTermsTextViewHandlers() {
        TextView terms_view = (TextView) findViewById(R.id.yandexTerms);
        terms_view.setLinksClickable(true);
        terms_view.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private ArrayList<WordListItem> learningWord_;
    private ArrayAdapter<WordListItem> listAdapter_;
    private WordContext currentWord_;
    private Database db_;
}
