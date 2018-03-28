package backend;

/**
 * Created by as.grebennikov on 28.03.18.
 */

public interface Database {
    public void Open(String version);

    public void SaveWord(WordContext wordContext);
}
