package backend;

import androidx.annotation.Nullable;

/**
 * Created by as.grebennikov on 28.03.18.
 */

public interface Database {
    public void Open(String path, String version);

    public WordContext GetNextWord(@Nullable WordContext prevWord);
    public void SaveWord(WordContext wordContext) throws Exception;
    public void RemoveWord(WordContext wordContext);

    public void SaveRewindPoint(String fileId, long cursorPos);
    public long GetRewindPointBefore(String fileId, long beforePos);

    public boolean IsShowedTooltip(String componentId, String tooltipId);
    public void SetTooltipAsShowed(String componentId, String tooltipId);
}
