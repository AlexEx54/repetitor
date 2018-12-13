package com.projects.asgrebennikov.repetitor

import android.annotation.SuppressLint
import android.graphics.Color
import android.support.constraint.ConstraintLayout
import android.support.constraint.Guideline
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.BounceInterpolator
import android.view.animation.TranslateAnimation
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView

import java.util.ArrayList
import java.util.Timer
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
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
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
    private var tourGuide_: TourGuide? = null
    private var currentActiveTooltip_: String? = null
    private lateinit var interstitialAd_: InterstitialAd

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

            val db_version = R.string.db_version

            db_!!.Open(filesDir.toString(), getResources().getString(R.string.db_version));

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

        interstitialAd_ = InterstitialAd(this)
        interstitialAd_.adUnitId = "ca-app-pub-3940256099942544/1033173712"
        interstitialAd_.loadAd(AdRequest.Builder().build())
        interstitialAd_.adListener = object : AdListener() {
            override fun onAdClosed() {
                interstitialAd_.loadAd(AdRequest.Builder().build())
            }
        }

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
                backgroundColor { Color.parseColor("#99000000") }
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
        val component_name = "sentence_activity"

        val fromBottomAnimation = TranslateAnimation(0f, 0f, 200f, 0f)
                .apply {
                    duration = 1000
                    fillAfter = false
                    interpolator = BounceInterpolator()
                }

        val alphaAnimation = AlphaAnimation(0f, 1f)
                .apply {
                    duration = 600
                    fillAfter = false
                }

        if (!currentActiveTooltip_.isNullOrEmpty()) {
            db_!!.SetTooltipAsShowed(component_name, currentActiveTooltip_);

            if (currentActiveTooltip_ == "scale_sentence_explained") {
                SetTextViewHandlers()
            }
        }

        // 1. Explain sentence
        if (!db_!!.IsShowedTooltip(component_name, "this_is_sentence")) {
            val textView = findViewById<View>(R.id.sentenceTextView) as TextView

            tourGuide_!!.toolTip!!.setTitle("Привет! \n Это предложение, которое тебе нужно перевести на английский");
            tourGuide_!!.toolTip!!.setDescription("[ЖМИ СЮДА]")
            tourGuide_!!.toolTip!!.setGravity(Gravity.BOTTOM or Gravity.CENTER);
            tourGuide_!!.toolTip!!.setEnterAnimation(fromBottomAnimation)
            currentActiveTooltip_ = "this_is_sentence"
            tourGuide_!!.playOn(textView)
            return
        }

        // 2. Explain words list
        if (!db_!!.IsShowedTooltip(component_name, "this_is_words_list")) {
            val listView = findViewById<View>(R.id.wordsListView) as ListView

            tourGuide_!!.toolTip!!.setTitle("Words list header");
            tourGuide_!!.toolTip!!.setTitle("Если забыл как переводится какое-либо слово - пользуйся списком! Слова, на которые нажал, заносятся в список для повторения :)");
            tourGuide_!!.toolTip!!.setDescription("")
            tourGuide_!!.toolTip!!.setGravity(Gravity.TOP or Gravity.CENTER);
            tourGuide_!!.toolTip!!.setEnterAnimation(alphaAnimation)
            currentActiveTooltip_ = "this_is_words_list"
            tourGuide_!!.playOn(listView)
            return
        }

        // 3. Explain next button
        if (!db_!!.IsShowedTooltip(component_name, "this_is_next_button")) {
            val button = findViewById<View>(R.id.nextButton)

            tourGuide_!!.toolTip!!.setTitle("Чтобы сравнить свой перевод с оригинальным - жми на эту кнопку");
            tourGuide_!!.toolTip!!.setGravity(Gravity.BOTTOM or Gravity.LEFT);
            currentActiveTooltip_ = "this_is_next_button"
            tourGuide_!!.playOn(button)
            return
        }

        // 4. Explain sentence rewind backward or forward
        if (!db_!!.IsShowedTooltip(component_name, "rewind_sentence_explained")) {
            val textView = findViewById<View>(R.id.sentenceTextView) as TextView

            tourGuide_!!.toolTip!!.setTitle("Если что то пошло не так, и тексты не совпадают, можно смахнуть вправо или влево и \"перемотать\" предложение" );
            tourGuide_!!.toolTip!!.setGravity(Gravity.BOTTOM or Gravity.CENTER);
            currentActiveTooltip_ = "rewind_sentence_explained"
            tourGuide_!!.playOn(textView)
            return
        }

        // 5. Explain sentence field scale up and down
        if (!db_!!.IsShowedTooltip(component_name, "scale_sentence_explained")) {
            val textView = findViewById<View>(R.id.sentenceTextView) as TextView

            tourGuide_!!.toolTip!!.setTitle("Можно смахнуть вверх или вниз, чтобы увеличить или уменьшить текст предложения. Удачи!" );
            tourGuide_!!.toolTip!!.setGravity(Gravity.BOTTOM or Gravity.CENTER);
            currentActiveTooltip_ = "scale_sentence_explained"
            tourGuide_!!.playOn(textView)
            return
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
                            if (interstitialAd_.isLoaded) {
                                interstitialAd_.show()
                            }

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

        nextSentenceButton.setOnClickListener {
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
