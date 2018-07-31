package com.projects.asgrebennikov.repetitor;

/**
 * Created by as.grebennikov on 22.02.18.
 */


import android.support.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.util.Vector;

import backend.Vocabulary;
import backend.Word;


public class WordListItem {

    public WordListItem(@NonNull Word word, Vocabulary.TranslateDirection translateDirection) {
        word_ = word;
        setTranslations(null);
        setFolded(true);
        translateDirection_ = translateDirection;
        wordAppendix_ = new String();
        errorOccurred_ = false;
    }


    public Word getWord() {
        return word_;
    }


    public Vector<Word> getTranslations() {
        return translations_;
    }


    public void setTranslations(Vector<Word> translations) {
        this.translations_ = translations;
    }


    public boolean isFolded() {
        return folded_;
    }


    public void setFolded(boolean folded) {
        this.folded_ = folded;
    }


    public void setErrorOccurred(boolean value) {
        errorOccurred_ = value;
    }


    public Vocabulary.TranslateDirection getTranslateDirection() {
        return translateDirection_;
    }

    public void setTranslateDirection(Vocabulary.TranslateDirection translateDirection) {
        this.translateDirection_ = translateDirection;
    }

    public void setWordAppendix(String appendix) {
        wordAppendix_ = appendix;
    }

    @Override
    public String toString() {
        String result = StringUtils.capitalize(word_.GetText());
        result += wordAppendix_;

        if (!folded_) {
            result += "\n -------------------------- \n";

            if (errorOccurred_ || (translations_ == null)) {
                result += "Error occurred :(";
                return result;
            }

            int DISPLAY_TRANSLATIONS_LIMIT = 3;

            int printed_translations = 0;
            for (Word translation: translations_) {
                if (printed_translations > DISPLAY_TRANSLATIONS_LIMIT) {
                    break;
                }
                printed_translations++;
                result += StringUtils.capitalize(translation.GetText()) + "\n";
            }
        }

        return result;
    }


    private Word word_;
    private Vector<Word> translations_;
    private boolean folded_;
    private boolean errorOccurred_;
    private Vocabulary.TranslateDirection translateDirection_;
    private String wordAppendix_;


}
