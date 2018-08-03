package backend;

import android.support.annotation.Nullable;

/**
 * Created by as.grebennikov on 28.03.18.
 */

public interface Database {
    public void Open(String path, String version);

    public WordContext GetNextWord(@Nullable WordContext prevWord);
    public void SaveWord(WordContext wordContext);
    public void RemoveWord(WordContext wordContext);
    public void SaveRewindPoint(String fileId, long cursorPos);
    public long GetRewindPointBefore(long beforePos);
}
