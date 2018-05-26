package backend;

import org.apache.commons.lang3.ArrayUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import utils.SizedStack;

/**
 * Created by as.grebennikov on 09.05.18.
 */

public class TextSupplierImpl implements TextSupplier {

    public TextSupplierImpl(String filesDir,
                            InputStream fileStream,
                            String fileName) throws IOException {
        fileName_ = fileName;
        rawStream_ = fileStream;
        fileStream_ = new InputStreamReader(fileStream, "UTF-8");
        filesDir_ = filesDir;
        rewindPoints_ = new SizedStack<Long>(30);

        assert fileStream_.ready();
    }


    public Sentence GetNextSentence() {
        final StringBuilder sentenceStrBuilder = new StringBuilder();
        int cursorIncrement = 0;
        boolean canReadFurther = true;
        try {
            for (; ; ) {
                int charAsInt = fileStream_.read(); // TODO: eof handling
                if (charAsInt < 0) { // indication of read error
                    canReadFurther = false;
                    break;
                }
                cursorIncrement++;
                char char_ = (char) charAsInt;
                if (IsSentenceDelimiter(char_)) {
                    break;
                }
                sentenceStrBuilder.append(char_);
            }
        } catch (IOException e) {
            return null;
        }

        sentenceStartPos_ = sentenceEndPos_;
        sentenceEndPos_ += cursorIncrement;

        String sentenceAsStr = sentenceStrBuilder.toString();

        if (sentenceAsStr.isEmpty() && canReadFurther) {
            return GetNextSentence();
        }

        rewindPoints_.push(sentenceStartPos_);

        sentenceAsStr = sentenceAsStr.replaceAll("(\\r|\\n)", " ");

        currentSentence_ = new SentenceImpl(sentenceAsStr);
        return currentSentence_;
    }


    public Sentence GetPrevSentence() {
        if (rewindPoints_.empty()) {
            return null;
        }

        try {
            rewindPoints_.pop();

            long prevSentencePos = 0;
            if (rewindPoints_.empty()) {
                prevSentencePos = 0;
            } else {
                prevSentencePos = rewindPoints_.pop();
            }

            SeekToPosition(prevSentencePos);
        } catch (Exception e) {
            return null;
        }

        currentSentence_ = GetNextSentence();
        return currentSentence_;
    }


    public Sentence GetCurrentSentence() {
        return currentSentence_;
    }


    public boolean SaveCursor() {
        BufferedWriter out = null;

        try {
            FileWriter file = new FileWriter(filesDir_ + "/" + fileName_ + "__cursor", false);
            out = new BufferedWriter(file);
            out.write(Long.toString(sentenceStartPos_));
            out.close();
        } catch (Exception e) {
            return false;
        }

        return true;
    }


    public boolean LoadCursor() {
        try {
            long loadedCursor = LoadCursorValue();
            FillUpRewindPoints(loadedCursor);
            SeekToPosition(loadedCursor);
        } catch (Exception e) {
            return false;
        }

        return true;
    }


    private boolean IsSentenceDelimiter(char c) {
        return ArrayUtils.contains(sentenceDelimiters_, c);
    }


    private long LoadCursorValue() throws IOException {
        File file = new File(filesDir_ + "/" + fileName_ + "__cursor");
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String text = reader.readLine();
        if (text != null) {
            return (long) Integer.parseInt(text);
        }

        return 0;
    }

    // fills points up to topPosition
    private void FillUpRewindPoints(long topPosition) throws IOException {
        ResetFileStreamPosition();

        while (sentenceEndPos_ < topPosition) {
            // pushes rewind points on every GetNextSentence
            Sentence readSentence = GetNextSentence();
            if (readSentence == null) {
                break;
            }
        }

        ResetFileStreamPosition();
    }


    private void SeekToPosition(long position) throws IOException {
        ResetFileStreamPosition();
        fileStream_.skip(position);
        sentenceStartPos_ = position;
        sentenceEndPos_ = position;
    }


    private void ResetFileStreamPosition() throws IOException {
        rawStream_.reset();
        fileStream_ = new InputStreamReader(rawStream_, "UTF-8");
        sentenceStartPos_ = 0;
        sentenceEndPos_ = 0;
    }


    private long sentenceStartPos_ = 0; // offsets in characters
    private long sentenceEndPos_ = 0;
    private InputStream rawStream_;
    private String fileName_;
    private InputStreamReader fileStream_;
    private String filesDir_;
    private SizedStack<Long> rewindPoints_;
    private Sentence currentSentence_;

    private final char[] sentenceDelimiters_ = {'.', ';'};
}
