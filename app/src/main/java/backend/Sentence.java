package backend;

import java.util.Vector;

/**
 * Created by as.grebennikov on 15.01.18.
 */

public interface Sentence {

    public Vector<Word> GetWords();
    public String AsString();
}
