package backend;

import java.util.Vector;

/**
 * Created by as.grebennikov on 21.02.18.
 */


public interface Vocabulary {

    enum TranslateDirection {
        RU_EN,
        EN_RU,
        AUTO
    }

    public Vector<Word> Translate(Word word, TranslateDirection direction);
}


