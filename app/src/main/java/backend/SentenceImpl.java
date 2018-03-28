package backend;

import java.util.Arrays;
import java.util.Vector;

/**
 * Created by as.grebennikov on 15.01.18.
 */

import io.reactivex.*;
import io.reactivex.schedulers.Schedulers;


public final class SentenceImpl implements Sentence {

    private final String sentence_;
    private static Vector<String> russianPreps_ = new Vector<String>(Arrays.asList(
                                                                                    "в",
                                                                                    "без",
                                                                                    "до",
                                                                                    "из",
                                                                                    "к",
                                                                                    "на",
                                                                                    "по",
                                                                                    "о",
                                                                                    "от",
                                                                                    "перед",
                                                                                    "при",
                                                                                    "через",
                                                                                    "с",
                                                                                    "у",
                                                                                    "и",
                                                                                    "б",
                                                                                    "нет",
                                                                                    "за",
                                                                                    "над",
                                                                                    "для",
                                                                                    "об",
                                                                                    "под",
                                                                                    "про"
                                        ));

    public SentenceImpl(String sentence) {
        sentence_ = sentence;
    }


    public Vector<Word> GetWords() {
        // TODO: use streams after API 24 is available

        String[] words = sentence_.replaceAll("[^\\p{L}- ]", "").toLowerCase().
                split("\\s+");
        Vector<String> wordsAsList = new Vector<String>(Arrays.asList(words));

        return ToWords(RemoveRussianPreps(wordsAsList));
    }


    public String AsString() {
        return sentence_;
    }


    private Vector<String> RemoveRussianPreps( Vector<String> source ) {
        source.removeAll(russianPreps_);
        return source;
    }


    private Vector<Word> ToWords(Vector<String> wordsAsStrings) {
        Vector<Word> result = new Vector<Word>();

        for ( String word: wordsAsStrings ) {
            result.add(new Word(word));
        }

        return result;
    }
}
