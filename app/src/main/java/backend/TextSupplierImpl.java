package backend;

import android.provider.ContactsContract;

import org.apache.commons.lang3.ArrayUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import backend.Database;

import utils.SizedStack;

/**
 * Created by as.grebennikov on 09.05.18.
 */

public class TextSupplierImpl implements TextSupplier {

    public TextSupplierImpl(String filesDir,
                            InputStream fileStream,
                            String fileName,
                            Database db) throws IOException {
        fileName_ = fileName;
        rawStream_ = fileStream;
        fileStream_ = new InputStreamReader(fileStream, "UTF-8");
        filesDir_ = filesDir;
        db_ = db;

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
                if (IsSentenceDelimiter(char_) &&
                    !SentenceEndsWithExceptionalSequence(sentenceStrBuilder.toString()))
                {
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

        db_.SaveRewindPoint(fileName_, sentenceStartPos_);

        sentenceAsStr = sentenceAsStr.replaceAll("(\\r|\\n)", " ");

        currentSentence_ = new SentenceImpl(sentenceAsStr);
        return currentSentence_;
    }


    public Sentence GetPrevSentence() {
        try {
            long rewind_point = db_.GetRewindPointBefore(fileName_, sentenceStartPos_);
            SeekToPosition(rewind_point);
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

    private boolean SentenceEndsWithExceptionalSequence(String sentence) {
        if (sentence.length() < 4) {
            return true;
        }

        String lcSentence = sentence.toLowerCase();
        if (lcSentence.endsWith(" mrs") ||
            lcSentence.endsWith(" mr") ||
            lcSentence.endsWith(" miss") ||
            lcSentence.endsWith(" ms"))
        {
            return true;
        }

        return false;
    }


    private long sentenceStartPos_ = 0; // offsets in characters
    private long sentenceEndPos_ = 0;
    private InputStream rawStream_;
    private String fileName_;
    private InputStreamReader fileStream_;
    private String filesDir_;
    private Sentence currentSentence_;
    private Database db_;

    private final char[] sentenceDelimiters_ = {'.', ';'};
}
