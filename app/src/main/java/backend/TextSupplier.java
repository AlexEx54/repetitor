package backend;

/**
 * Created by as.grebennikov on 15.01.18.
 */

public interface TextSupplier {

    Sentence GetNextSentence();
    Sentence GetPrevSentence();

    public boolean SaveCursor();
    public boolean LoadCursor();
}
