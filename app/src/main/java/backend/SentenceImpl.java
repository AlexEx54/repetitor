package backend;

import java.util.Arrays;
import java.util.Scanner;
import java.util.Vector;

/**
 * Created by as.grebennikov on 15.01.18.
 */

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


    public Vector<String> GetWords() {
        String[] words = sentence_.replaceAll("[^\\p{L} ]", "").split("\\s+");
        Vector<String> wordsAsList = new Vector<String>(Arrays.asList(words));

        return RemoveRussianPreps(wordsAsList);
    }


    public String AsString() {
        return sentence_;
    }


    private Vector<String> RemoveRussianPreps( Vector<String> source ) {
        source.removeAll(russianPreps_);
        return source;
    }
}
