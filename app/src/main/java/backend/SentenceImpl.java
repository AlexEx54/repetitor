package backend;

import java.util.Vector;

/**
 * Created by as.grebennikov on 15.01.18.
 */

public final class SentenceImpl implements Sentence {

    public SentenceImpl(String sentence) {
        sentence_ = sentence;
    }


    public Vector<String> GetWords() {
        return new Vector<String>();
    }


    public String AsString() {
        return sentence_;
    }

    private final String sentence_;
}
