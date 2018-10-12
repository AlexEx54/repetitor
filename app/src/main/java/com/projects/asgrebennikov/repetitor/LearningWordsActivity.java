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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.Vector;

import backend.Database;
import backend.DatabaseImpl;
import backend.Sentence;
import backend.TextSupplier;
import backend.TextSupplierImpl;
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
        db_.Open(getFilesDir().toString(),"3.0.0");

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
        SetContainingSentenceClickHandler();

        ShowNextWordToLearn(null);
    }


    private void ShowNextWordToLearn(WordContext prevWord) {
        try {


            WordContext wordToLearn = db_.GetNextWord(prevWord);

            if ((wordToLearn == null) ||
                    (wordToLearn.word == null)) {

                SetNothingToLearnView();
                return;
            }

            InputStream primary_sentence_stream = getResources().openRawResource(
                    getResources().getIdentifier(wordToLearn.primarySentenceFileId, "raw", getPackageName()));
            containingSentenceSupplier = new TextSupplierImpl(
                    getFilesDir().getAbsolutePath(),
                    primary_sentence_stream,
                    wordToLearn.primarySentenceFileId,
                    db_);
            InputStream complementary_sentence_stream = getResources().openRawResource(
                    getResources().getIdentifier(wordToLearn.complementarySentenceFileId, "raw", getPackageName()));
            complementarySentenceSupplier = new TextSupplierImpl(
                    getFilesDir().getAbsolutePath(),
                    complementary_sentence_stream,
                    wordToLearn.complementarySentenceFileId,
                    db_);

            containingSentenceSupplier.SetCursorPos(wordToLearn.primarySentenceCursorPos);
            complementarySentenceSupplier.SetCursorPos(wordToLearn.complementarySentenceCursorPos);

            complementarySentenceSupplier.GetNextSentence();

            TextView containigTextView = (TextView) findViewById(R.id.containingSentenceTextView);
            containigTextView.setText(containingSentenceSupplier.GetNextSentence().AsString());

            TextView complementaryTextView = (TextView) findViewById(R.id.complementarySentenceTextView);
            complementaryTextView.setText("- - -");

            WordListItem listItem = new WordListItem(wordToLearn.word, wordToLearn.translateDirection);
            learningWord_.clear();
            learningWord_.add(listItem);
            listAdapter_.notifyDataSetChanged();

            currentWord_ = wordToLearn;
        } catch( Exception e )
        {
            return; // todo: show toast or something
        }
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
                    Timer timer = new Timer();
                    TranslationProgressIndicationTask timerTask =
                            new TranslationProgressIndicationTask(item, listAdapter_);
                    timer.scheduleAtFixedRate(timerTask, 0, 500);

                    YandexVocabularyImpl vocabulary = new YandexVocabularyImpl();
                    Vector<Word> translations = vocabulary.Translate(item.getWord(), item.getTranslateDirection());

                    timerTask.cancel();
                    timer.cancel();

                    if (translations == null) {
                        throw new Exception("Fuck!");
                    }

                    return translations;
                })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe((Vector<Word> words) -> {
                            item.setErrorOccurred(false);
                            item.setTranslations(words);
                            item.setFolded(!item.isFolded());
                            listAdapter_.notifyDataSetChanged();
                        },
                        (e) -> {
                            item.setFolded(!item.isFolded());
                            item.setErrorOccurred(true);
                            listAdapter_.notifyDataSetChanged();
                        });
            }
        });
    }


    private void SetComplementarySentenceClickHandler() {
        TextView textView = (TextView) findViewById(R.id.complementarySentenceTextView);

        textView.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public synchronized void onTouched() {
                if ((currentWord_ == null) || complementarySentenceSupplier == null) {
                    return;
                }

                textView.setText(complementarySentenceSupplier.GetCurrentSentence().AsString());
            }

            @Override
            public void onSwipeLeft() {
                if ((currentWord_ == null) ||
                     complementarySentenceSupplier == null ||
                     (textView.getText().toString().equalsIgnoreCase("- - -"))) {
                    return;
                }

                Sentence nextSentence = complementarySentenceSupplier.GetNextSentence();
                if (nextSentence == null)
                {
                    return;
                }

                textView.setText(nextSentence.AsString());
            }

            @Override
            public void onSwipeRight() {
                if ((currentWord_ == null) ||
                        complementarySentenceSupplier == null ||
                        (textView.getText().toString().equalsIgnoreCase("- - -"))) {
                    return;
                }

                Sentence prevSentence = complementarySentenceSupplier.GetPrevSentence();
                if (prevSentence == null)
                {
                    return;
                }

                textView.setText(prevSentence.AsString());
            }
        });
    }


    private void SetContainingSentenceClickHandler() {
        TextView textView = (TextView) findViewById(R.id.containingSentenceTextView);

        textView.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeLeft() {
                if ((currentWord_ == null) ||
                        containingSentenceSupplier == null) {
                    return;
                }

                Sentence nextSentence = containingSentenceSupplier.GetNextSentence();
                if (nextSentence == null)
                {
                    return;
                }

                textView.setText(nextSentence.AsString());
            }

            @Override
            public void onSwipeRight() {
                if ((currentWord_ == null) ||
                        containingSentenceSupplier == null) {
                    return;
                }

                Sentence prevSentence = containingSentenceSupplier.GetPrevSentence();
                if (prevSentence == null)
                {
                    return;
                }

                textView.setText(prevSentence.AsString());
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
    private TextSupplier containingSentenceSupplier;
    private TextSupplier complementarySentenceSupplier;
}
