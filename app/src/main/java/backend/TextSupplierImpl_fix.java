package backend;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * Created by as.grebennikov on 09.05.18.
 */

public class TextSupplierImpl_fix implements TextSupplier {

    public TextSupplierImpl_fix(String filesDir,
                                InputStream fileStream,
                                String fileName) throws IOException {
        fileName_ = fileName;
        fileStream_ = new InputStreamReader(fileStream, "UTF-8");
        filesDir_ = filesDir;

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

        cursor_ += cursorIncrement;
        String sentenceAsStr = sentenceStrBuilder.toString();

        if (sentenceAsStr.isEmpty() && canReadFurther) {
            return GetNextSentence();
        }

        return new SentenceImpl(sentenceStrBuilder.toString());
    }


    public boolean SaveCursor() {
        BufferedWriter out = null;

        try {
            FileWriter file = new FileWriter(filesDir_ + "/" + fileName_ + "__cursor", false);
            out = new BufferedWriter(file);
            out.write(Long.toString(cursor_));
            out.close();
        } catch (Exception e) {
            return false;
        }

        return true;
    }


    public boolean LoadCursor() {
        try {
            File file = new File(filesDir_ + "/" + fileName_ + "__cursor");
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String text = reader.readLine();
            if (text != null) {
                cursor_ = Integer.parseInt(text);
            }
            fileStream_.skip(cursor_);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private boolean IsSentenceDelimiter(char c) {
        return Arrays.asList(sentenceDelimiters_).contains(c);
    }


    private long cursor_ = 0; // offset in characters
    private String fileName_;
    private InputStreamReader fileStream_;
    private String filesDir_;

    private final char[] sentenceDelimiters_ = {'.', ';'};
}
