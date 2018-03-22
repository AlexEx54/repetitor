package com.projects.asgrebennikov.repetitor;

/**
 * Created by as.grebennikov on 22.02.18.
 */


import android.support.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.util.Vector;

import backend.Word;


public class WordListItem {

    public WordListItem(@NonNull Word word) {
        word_ = word;
        setTranslations(null);
        setFolded(true);
    }


    public Word getWord() {
        return word_;
    }


    public Vector<Word> getTranslations() {
        return translations_;
    }


    public void setTranslations(Vector<Word> translations_) {
        this.translations_ = translations_;
    }


    public boolean isFolded() {
        return folded_;
    }


    public void setFolded(boolean folded_) {
        this.folded_ = folded_;
    }

    @Override
    public String toString() {
        String result = StringUtils.capitalize(word_.GetText());

        if (!folded_ && (translations_ != null)) {
            result += "\n -------------------------- \n";

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


}
