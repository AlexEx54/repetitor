package backend;

/**
 * Created by as.grebennikov on 28.03.18.
 */

public class WordContext {
    public long timestamp;
    public Word word;
    public String primarySentenceFileId;
    public long primarySentenceCursorPos; // in charactes
    public String complementarySentenceFileId;
    public long complementarySentenceCursorPos; // in characters
    public Vocabulary.TranslateDirection translateDirection;
}
