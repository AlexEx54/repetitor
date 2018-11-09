package com.projects.asgrebennikov.repetitor

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.support.constraint.ConstraintLayout
import android.support.constraint.Guideline
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView

import java.io.Console
import java.io.InputStream
import java.util.ArrayList
import java.util.Timer
import java.util.TimerTask
import java.util.Vector

import backend.Database
import backend.DatabaseImpl
import backend.Sentence
import backend.TextSupplier
import backend.TextSupplierImpl
import backend.Vocabulary
import backend.Word
import backend.WordContext
import backend.YandexVocabularyImpl
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import tourguide.tourguide.TourGuide


class SentenceActivity : AppCompatActivity() {


    private var rusTextSupplier_: TextSupplier? = null
    private var engTextSupplier_: TextSupplier? = null
    private var wordsList_: ArrayList<WordListItem>? = null

    private var currentDirection_: Vocabulary.TranslateDirection? = null
    private var rusSentence_: Sentence? = null
    private var engSentence_: Sentence? = null
    private var db_: Database? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sentence)
        wordsList_ = ArrayList()

        SetWordListViewHandlers()
        SetNextButtonHandlers()
        SetTextViewHandlers()
        SetTermsTextViewHandlers()

        val passedIntent = intent
        val englishText = passedIntent.getStringExtra("eng_text_name")
        val russianText = passedIntent.getStringExtra("rus_text_name")

        try {
            db_ = DatabaseImpl()
            db_!!.Open(filesDir.toString(), "3.0.0")

            val rus_stream = resources.openRawResource(
                    resources.getIdentifier(russianText, "raw", packageName))
            val eng_stream = resources.openRawResource(
                    resources.getIdentifier(englishText, "raw", packageName))

            rusTextSupplier_ = TextSupplierImpl(filesDir.absolutePath, rus_stream, russianText, db_)
            engTextSupplier_ = TextSupplierImpl(filesDir.absolutePath, eng_stream, englishText, db_)

            //            rusTextSupplier_.SaveCursor();
            //            engTextSupplier_.SaveCursor();

            rusTextSupplier_!!.LoadCursor()
            engTextSupplier_!!.LoadCursor()

            rusSentence_ = rusTextSupplier_!!.GetNextSentence()
            engSentence_ = engTextSupplier_!!.GetNextSentence()

            currentDirection_ = Vocabulary.TranslateDirection.RU_EN

            val textView = findViewById<View>(R.id.sentenceTextView) as TextView
            textView.text = rusSentence_!!.AsString()
            val words = rusSentence_!!.GetWords()
            wordsList_!!.addAll(ToWordListItems(words, Vocabulary.TranslateDirection.RU_EN))
        } catch (e: Exception) {
            finish()
        }

    }

    private fun SetTermsTextViewHandlers() {
        val terms_view = findViewById<View>(R.id.yandexTerms) as TextView
        terms_view.linksClickable = true
        terms_view.movementMethod = LinkMovementMethod.getInstance()
    }


    private fun ToWordListItems(words: Vector<Word>,
                                translateDirection: Vocabulary.TranslateDirection?): Vector<WordListItem> {
        val result = Vector<WordListItem>()

        for (word in words) {
            result.add(WordListItem(word, translateDirection))
        }

        return result
    }


    private fun SetWordListViewHandlers() {
        val lv = findViewById<View>(R.id.wordsListView) as ListView
        val adapter = WordsArrayAdapter(this,
                android.R.layout.simple_list_item_1,
                wordsList_)
        lv.adapter = adapter

        lv.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>, view: View, position: Int,
                                     id: Long) {

                val item = parent.getItemAtPosition(position) as WordListItem
                if (item.translations != null) {
                    item.isFolded = !item.isFolded
                    adapter.notifyDataSetChanged()
                    return
                }

                Flowable.fromCallable {
                    val timer = Timer()
                    val timerTask = TranslationProgressIndicationTask(item, adapter)
                    timer.scheduleAtFixedRate(timerTask, 0, 500)

                    val vocabulary = YandexVocabularyImpl()
                    val translations = vocabulary.Translate(item.word, item.translateDirection)

                    timerTask.cancel()
                    timer.cancel()

                    if (translations == null)
                        throw Exception("FFFffffuuck")

                    translations
                }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ words: Vector<Word> ->
                            item.setErrorOccurred(false)
                            item.translations = words
                            item.isFolded = !item.isFolded
                            adapter.notifyDataSetChanged()
                            SaveWordToDb(item.word, item.translateDirection)

                        },
                                { e ->
                                    item.isFolded = !item.isFolded
                                    item.setErrorOccurred(true)
                                    adapter.notifyDataSetChanged()
                                })
            }


            @Throws(Exception::class)
            private fun SaveWordToDb(word: Word, translateDirection: Vocabulary.TranslateDirection) {
                val wordContext = WordContext()
                wordContext.timestamp = System.currentTimeMillis()
                wordContext.word = word
                wordContext.translateDirection = translateDirection

                if (currentDirection_ == Vocabulary.TranslateDirection.RU_EN) {
                    wordContext.primarySentenceFileId = rusTextSupplier_!!.GetFileId()
                    wordContext.primarySentenceCursorPos = rusTextSupplier_!!.GetCursorPos()
                    wordContext.complementarySentenceFileId = engTextSupplier_!!.GetFileId()
                    wordContext.complementarySentenceCursorPos = engTextSupplier_!!.GetCursorPos()
                } else {
                    wordContext.primarySentenceFileId = engTextSupplier_!!.GetFileId()
                    wordContext.primarySentenceCursorPos = engTextSupplier_!!.GetCursorPos()
                    wordContext.complementarySentenceFileId = rusTextSupplier_!!.GetFileId()
                    wordContext.complementarySentenceCursorPos = rusTextSupplier_!!.GetCursorPos()
                }

                db_!!.SaveWord(wordContext)
            }
        }
    }


    private fun SetNextButtonHandlers() {
        val nextSentenceButton = findViewById<View>(R.id.nextButton) as Button

//        val tourGuide: TourGuide = TourGuide.create(this) {
//            pointer {}
//
//            toolTip {
//                title { "Welcome!" }
//                description { "Click on Get Started to begin..." }
//            }
//
//            overlay {
//                backgroundColor { Color.parseColor("#66FF0000") }
//            }
//        }.playOn(nextSentenceButton)

        nextSentenceButton.setOnClickListener {
//            tourGuide.cleanUp();

            var sentence: Sentence? = null

            if (currentDirection_ == Vocabulary.TranslateDirection.RU_EN) {
                currentDirection_ = Vocabulary.TranslateDirection.EN_RU
                sentence = engSentence_
            } else {
                engSentence_ = engTextSupplier_!!.GetNextSentence()
                rusSentence_ = rusTextSupplier_!!.GetNextSentence()
                currentDirection_ = Vocabulary.TranslateDirection.RU_EN
                sentence = rusSentence_
            }

            rusTextSupplier_!!.SaveCursor()
            engTextSupplier_!!.SaveCursor()

            val textView = findViewById<View>(R.id.sentenceTextView) as TextView
            val lv = findViewById<View>(R.id.wordsListView) as ListView

            textView.text = sentence!!.AsString()
            val words = sentence.GetWords()
            val adapter = lv.adapter as ArrayAdapter<Word>
            wordsList_!!.clear()
            wordsList_!!.addAll(ToWordListItems(words, currentDirection_))
            adapter.notifyDataSetChanged()
        }
    }


    private fun SetTextViewHandlers() {
        val textView = findViewById<View>(R.id.sentenceTextView) as TextView
        textView.gravity = Gravity.CENTER

        val tourGuide: TourGuide = TourGuide.create(this) {
            pointer {
                color { Color.parseColor("#FFFF0000") }
            }

            toolTip {
                title { "Привет!" }
                description { "Немного объясним как здесь что :) Это предложение) Как бы ты сказал его на английком? Жми на этот текст " }
            }

            overlay {
                backgroundColor { Color.parseColor("#99000000") }
            }
        };

        tourGuide.toolTip!!.setOnClickListener( object : View.OnClickListener {
            override fun onClick(view: View) {
                tourGuide.cleanUp();
            }
        });

        tourGuide.playOn(textView)


        val lv = findViewById<View>(R.id.wordsListView) as ListView

        textView.setOnTouchListener(object : OnSwipeTouchListener(this) {
            override fun onSwipeLeft() {
                var currentSupplier: TextSupplier? = null


                when (currentDirection_) {
                    Vocabulary.TranslateDirection.RU_EN -> {
                        currentSupplier = rusTextSupplier_
                    }
                    Vocabulary.TranslateDirection.EN_RU -> {
                        currentSupplier = engTextSupplier_
                    }
                }

                var currentSentence: Sentence? = null
                currentSentence = currentSupplier!!.GetNextSentence()
                currentSupplier.SaveCursor()

                textView.text = currentSentence!!.AsString()
                val adapter = lv.adapter as ArrayAdapter<Word>
                wordsList_!!.clear()
                wordsList_!!.addAll(ToWordListItems(currentSentence.GetWords(), currentDirection_))
                adapter.notifyDataSetChanged()
            }

            override fun onSwipeRight() {
                var currentSupplier: TextSupplier? = null

                when (currentDirection_) {
                    Vocabulary.TranslateDirection.RU_EN -> {
                        currentSupplier = rusTextSupplier_
                    }
                    Vocabulary.TranslateDirection.EN_RU -> {
                        currentSupplier = engTextSupplier_
                    }
                }

                var currentSentence: Sentence? = null
                currentSentence = currentSupplier!!.GetPrevSentence()

                if (currentSentence == null) {
                    return
                }

                currentSupplier!!.SaveCursor()

                textView.text = currentSentence!!.AsString()
                val adapter = lv.adapter as ArrayAdapter<Word>
                wordsList_!!.clear()
                wordsList_!!.addAll(ToWordListItems(currentSentence!!.GetWords(), currentDirection_))
                adapter.notifyDataSetChanged()
            }

            override fun onSwipeBottom() {
                val guideLine = findViewById<View>(R.id.guideline2) as Guideline
                val params = guideLine.layoutParams as ConstraintLayout.LayoutParams
                params.guidePercent = 0.5f
                guideLine.layoutParams = params
            }

            override fun onSwipeTop() {
                val guideLine = findViewById<View>(R.id.guideline2) as Guideline
                val params = guideLine.layoutParams as ConstraintLayout.LayoutParams
                params.guidePercent = 0.2f
                guideLine.layoutParams = params
            }
        })
    }


    private fun PassTextToGTranslateApp() {
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

} // class SentenceActivity
