package backend;

/**
 * Created by as.grebennikov on 28.03.18.
 */

public class WordContext {
    public String contextId;
    public Word word;
    public Sentence containingSentence;
    public Sentence complementarySentence;
    public Vocabulary.TranslateDirection translateDirection;
}
