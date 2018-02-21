package backend;

/**
 * Created by as.grebennikov on 21.02.18.
 */

public class Word {

    public enum WordType {
        UNKNOWN
    }


    public Word(String text) {
        text_ = text;
        type_ = WordType.UNKNOWN;
    }


    public String GetText() {
        return text_;
    }


    public WordType GetWordType() {
        return type_;
    }


    private String text_;
    private WordType type_;
}
