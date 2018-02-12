package backend;

import java.util.Arrays;
import java.util.Scanner;
import java.util.Vector;

/**
 * Created by as.grebennikov on 15.01.18.
 */

public final class SentenceImpl implements Sentence {

    public SentenceImpl(String sentence) {
        sentence_ = sentence;
    }


    public Vector<String> GetWords() {
        String[] words = sentence_.replaceAll("[^\\p{L} ]", "").split("\\s+");
        return new Vector<String>(Arrays.asList(words));
    }


    public String AsString() {
        return sentence_;
    }

    private final String sentence_;
}
