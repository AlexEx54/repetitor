package backend;

/**
 * Created by as.grebennikov on 15.01.18.
 */

public interface TextSupplier {

    Sentence GetNextSentence();
    Sentence GetPrevSentence();

    Sentence GetCurrentSentence();

    public boolean SaveCursor();
    // Loads cursor from saved state
    public boolean LoadCursor();
    // Sets fixed cursor position
    public boolean SetCursorPos(long pos);
    public long GetCursorPos();

    public String GetFileId();
}
