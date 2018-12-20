package com.projects.asgrebennikov.repetitor

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView

import java.io.InputStream
import java.util.ArrayList
import java.util.Timer
import java.util.Vector

import backend.Database
import backend.DatabaseImpl
import backend.Sentence
import backend.TextSupplier
import backend.TextSupplierImpl
import backend.Word
import backend.WordContext
import backend.YandexVocabularyImpl
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import tourguide.tourguide.TourGuide

class LearningWordsActivity : AppCompatActivity() {

    private var learningWord_: ArrayList<WordListItem>? = null
    private var listAdapter_: ArrayAdapter<WordListItem>? = null
    private var currentWord_: WordContext? = null
    private var db_: Database? = null
    private var containingSentenceSupplier: TextSupplier? = null
    private var complementarySentenceSupplier: TextSupplier? = null

    private var tourGuide_: TourGuide? = null
    private var currentActiveTooltip_: String? = null

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.learning_words)

        db_ = DatabaseImpl()
        db_!!.Open(getFilesDir().toString(), getResources().getString(R.string.db_version))

        learningWord_ = ArrayList<WordListItem>()
        val learningWordsList = findViewById<View>(R.id.lw_learningWordList) as ListView
        listAdapter_ = WordsArrayAdapter<WordListItem>(this,
                android.R.layout.simple_list_item_1,
                learningWord_)
        learningWordsList.setAdapter(listAdapter_)

        SetListViewClickHandler(learningWordsList)
        SetComplementarySentenceClickHandler()
        SetNextWordClickHandler()
        SetCompletedWordButtonHandler()
        SetTermsTextViewHandlers()
        SetContainingSentenceClickHandler()

        ShowNextWordToLearn(null)

        SetupTooltipAndHandlers();
    }


    private fun SetupTooltipAndHandlers() {
        tourGuide_ = TourGuide.create(this) {
            pointer {
                color { Color.parseColor("#FFFF0000") }
            }

            toolTip {
                gravity { Gravity.TOP }
            }

            overlay {
                backgroundColor { Color.parseColor("#99500000") }
                disableClick { true }
                disableClickThroughHole { true }
            }
        };

        tourGuide_!!.toolTip!!.setOnClickListener( object : View.OnClickListener {
            override fun onClick(view: View) {
                tourGuide_!!.cleanUp();
                ProceedTooltip();
            }
        });

        ProceedTooltip()
    }


    private fun ProceedTooltip() {
        val component_name = "learning_words_activity"

        if (currentWord_ == null) {
            return
        }

        val alphaAnimation = AlphaAnimation(0f, 1f)
                .apply {
                    duration = 600
                    fillAfter = false
                }

        if (!currentActiveTooltip_.isNullOrEmpty()) {
            db_!!.SetTooltipAsShowed(component_name, currentActiveTooltip_);

            if ( currentActiveTooltip_ == "this_is_next_button") {
                SetComplementarySentenceClickHandler()
                SetContainingSentenceClickHandler()
            }
        }

        // 1. Explain word
        if (!db_!!.IsShowedTooltip(component_name, "this_is_word_to_learn")) {
            val view = findViewById<View>(R.id.lw_learningWordList) as View

            tourGuide_!!.toolTip!!.setTitle("Попытайся вспомнить как переводится слово. " +
                                            "Если не получается, на него можно нажать и получить перевод");
            tourGuide_!!.toolTip!!.setDescription("[ЖМИ СЮДА]")
            tourGuide_!!.toolTip!!.setGravity(Gravity.TOP or Gravity.CENTER);
            tourGuide_!!.toolTip!!.setEnterAnimation(alphaAnimation)
            currentActiveTooltip_ = "this_is_word_to_learn"
            tourGuide_!!.playOn(view)
            return
        }

        // 2. Explain containing sentence
        if (!db_!!.IsShowedTooltip(component_name, "this_is_containing_sentence")) {
            val view = findViewById<View>(R.id.containingSentenceTextView) as View

            tourGuide_!!.toolTip!!.setTitle("Это предложение, в котором употреблялось данное слово");
            tourGuide_!!.toolTip!!.setDescription("")
            tourGuide_!!.toolTip!!.setGravity(Gravity.BOTTOM or Gravity.CENTER);
            currentActiveTooltip_ = "this_is_containing_sentence"
            tourGuide_!!.playOn(view)
            return
        }

        // 3. Explain complementary sentence
        if (!db_!!.IsShowedTooltip(component_name, "this_is_complementary_sentence")) {
            val view = findViewById<View>(R.id.complementarySentenceTextView) as View

            tourGuide_!!.toolTip!!.setTitle("Нажмешь сюда, и получишь перевод исходного предложения. " +
                                            "Можно смахнуть вправо или влево, если перевод некорректный");
            tourGuide_!!.toolTip!!.setGravity(Gravity.TOP or Gravity.CENTER);
            currentActiveTooltip_ = "this_is_complementary_sentence"
            tourGuide_!!.playOn(view)
            return
        }

        // 4. Explain completed button
        if (!db_!!.IsShowedTooltip(component_name, "this_is_completed_button")) {
            val view = findViewById<View>(R.id.completedWordButton) as View

            tourGuide_!!.toolTip!!.setTitle("Если сам смог вспомнить перевод слова, нажми на эту кнопку - слово исчезнет " +
                                            "из списка для повторения");
            tourGuide_!!.toolTip!!.setGravity(Gravity.TOP or Gravity.RIGHT);
            currentActiveTooltip_ = "this_is_completed_button"
            tourGuide_!!.playOn(view)
            return
        }

        // 5. Explain next button
        if (!db_!!.IsShowedTooltip(component_name, "this_is_next_button")) {
            val view = findViewById<View>(R.id.nextWordButton) as View

            tourGuide_!!.toolTip!!.setTitle("Если не смог - пусть остается, жми на эту кнопку, чтобы показать следующее слово для повторения");
            tourGuide_!!.toolTip!!.setGravity(Gravity.TOP or Gravity.LEFT);
            currentActiveTooltip_ = "this_is_next_button"
            tourGuide_!!.playOn(view)
            return
        }
    }


    private fun ShowNextWordToLearn(prevWord: WordContext?) {
        try {


            val wordToLearn = db_!!.GetNextWord(prevWord)

            if (((wordToLearn == null) || (wordToLearn!!.word == null))) {

                SetNothingToLearnView()
                return
            }

            val primary_sentence_stream = getResources().openRawResource(
                    getResources().getIdentifier(wordToLearn!!.primarySentenceFileId, "raw", getPackageName()))
            containingSentenceSupplier = TextSupplierImpl(
                    getFilesDir().getAbsolutePath(),
                    primary_sentence_stream,
                    wordToLearn!!.primarySentenceFileId,
                    db_)
            val complementary_sentence_stream = getResources().openRawResource(
                    getResources().getIdentifier(wordToLearn!!.complementarySentenceFileId, "raw", getPackageName()))
            complementarySentenceSupplier = TextSupplierImpl(
                    getFilesDir().getAbsolutePath(),
                    complementary_sentence_stream,
                    wordToLearn!!.complementarySentenceFileId,
                    db_)

            containingSentenceSupplier!!.SetCursorPos(wordToLearn!!.primarySentenceCursorPos)
            complementarySentenceSupplier!!.SetCursorPos(wordToLearn!!.complementarySentenceCursorPos)

            complementarySentenceSupplier!!.GetNextSentence()

            val containigTextView = findViewById<View>(R.id.containingSentenceTextView) as TextView
            containigTextView.setText(containingSentenceSupplier!!.GetNextSentence().AsString())

            val complementaryTextView = findViewById<View>(R.id.complementarySentenceTextView) as TextView
            complementaryTextView.setText("- - -")

            val listItem = WordListItem(wordToLearn!!.word, wordToLearn!!.translateDirection)
            learningWord_!!.clear()
            learningWord_!!.add(listItem)
            listAdapter_!!.notifyDataSetChanged()

            currentWord_ = wordToLearn
        } catch (e: Exception) {
            return  // todo: show toast or something
        }

    }


    private fun SetNothingToLearnView() {
        val containigTextView = findViewById<View>(R.id.containingSentenceTextView) as TextView
        val complementaryTextView = findViewById<View>(R.id.complementarySentenceTextView) as TextView

        containigTextView.setText("Нет слов для \n повторения \n ☺")
        complementaryTextView.setText("- - -")
        learningWord_!!.clear()
        listAdapter_!!.notifyDataSetChanged()
    }


    private fun SetListViewClickHandler(listView: ListView) {

        listView.setOnItemClickListener(object : AdapterView.OnItemClickListener {
            public override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val item = parent.getItemAtPosition(position) as WordListItem
                if (item.getTranslations() != null) {
                    item.setFolded(!item.isFolded())
                    listAdapter_!!.notifyDataSetChanged()
                    return
                }

                Flowable.fromCallable<Vector<Word>>({
                    val timer = Timer()
                    val timerTask = TranslationProgressIndicationTask(item, listAdapter_)
                    timer.scheduleAtFixedRate(timerTask, 0, 500)

                    val vocabulary = YandexVocabularyImpl()
                    val translations = vocabulary.Translate(item.getWord(), item.getTranslateDirection())

                    timerTask.cancel()
                    timer.cancel()

                    if (translations == null) {
                        throw Exception("Fuck!")
                    }

                    translations
                })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ words: Vector<Word> ->
                            item.setErrorOccurred(false)
                            item.setTranslations(words)
                            item.setFolded(!item.isFolded())
                            listAdapter_!!.notifyDataSetChanged()
                        },
                                { e ->
                                    item.setFolded(!item.isFolded())
                                    item.setErrorOccurred(true)
                                    listAdapter_!!.notifyDataSetChanged()
                                })
            }
        })
    }


    private fun SetComplementarySentenceClickHandler() {
        val textView = findViewById<View>(R.id.complementarySentenceTextView) as TextView

        textView.setOnTouchListener(object : OnSwipeTouchListener(this) {
            @Synchronized
            public override fun onTouched() {
                if ((currentWord_ == null) || complementarySentenceSupplier == null) {
                    return
                }

                textView.setText(complementarySentenceSupplier!!.GetCurrentSentence().AsString())
            }

            public override fun onSwipeLeft() {
                if (((currentWord_ == null) ||
                                complementarySentenceSupplier == null ||
                                (textView.getText().toString().equals("- - -", ignoreCase = true)))) {
                    return
                }

                val nextSentence = complementarySentenceSupplier!!.GetNextSentence()
                if (nextSentence == null) {
                    return
                }

                textView.setText(nextSentence!!.AsString())
            }

            public override fun onSwipeRight() {
                if (((currentWord_ == null) ||
                                complementarySentenceSupplier == null ||
                                (textView.getText().toString().equals("- - -", ignoreCase = true)))) {
                    return
                }

                val prevSentence = complementarySentenceSupplier!!.GetPrevSentence()
                if (prevSentence == null) {
                    return
                }

                textView.setText(prevSentence!!.AsString())
            }
        })
    }


    private fun SetContainingSentenceClickHandler() {
        val textView = findViewById<View>(R.id.containingSentenceTextView) as TextView

        textView.setOnTouchListener(object : OnSwipeTouchListener(this) {
            public override fun onSwipeLeft() {
                if (((currentWord_ == null) || containingSentenceSupplier == null)) {
                    return
                }

                val nextSentence = containingSentenceSupplier!!.GetNextSentence()
                if (nextSentence == null) {
                    return
                }

                textView.setText(nextSentence!!.AsString())
            }

            public override fun onSwipeRight() {
                if (((currentWord_ == null) || containingSentenceSupplier == null)) {
                    return
                }

                val prevSentence = containingSentenceSupplier!!.GetPrevSentence()
                if (prevSentence == null) {
                    return
                }

                textView.setText(prevSentence!!.AsString())
            }
        })
    }


    private fun SetNextWordClickHandler() {
        val nextWordButton = findViewById<View>(R.id.nextWordButton) as Button

        nextWordButton.setOnClickListener(object : View.OnClickListener {
            @Synchronized
            public override fun onClick(v: View) {
                ShowNextWordToLearn(currentWord_)
            }
        })
    }


    private fun SetCompletedWordButtonHandler() {
        val button = findViewById<View>(R.id.completedWordButton) as Button

        button.setOnClickListener(object : View.OnClickListener {
            @Synchronized
            public override fun onClick(v: View) {
                if (currentWord_ == null) {
                    return
                }

                db_!!.RemoveWord(currentWord_)
                ShowNextWordToLearn(currentWord_)
            }
        })
    }


    private fun SetTermsTextViewHandlers() {
        val terms_view = findViewById<View>(R.id.yandexTerms) as TextView
        terms_view.setLinksClickable(true)
        terms_view.setMovementMethod(LinkMovementMethod.getInstance())
    }
}
