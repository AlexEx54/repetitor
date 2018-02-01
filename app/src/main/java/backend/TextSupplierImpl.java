package backend;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by as.grebennikov on 15.01.18.
 */

public final class TextSupplierImpl implements TextSupplier {

    public TextSupplierImpl(String filesDir,
                            InputStream fileStream,
                            String fileName) throws IOException {
        fileName_ = fileName;
        fileStream_ = fileStream;
        filesDir_ = filesDir;
    }

    public Sentence GetNextSentence() {

        fileStream_.

        String sentenceStr = null;
        return new SentenceImpl(sentenceStr);
    }


    public boolean SaveCursor() {
        BufferedWriter out = null;

        try {
            FileWriter file = new FileWriter(filesDir_ + "/" + fileName_ + "__cursor", false);
            out = new BufferedWriter(file);
            out.write(Integer.toString(cursor_));
            out.close();
        } catch (Exception e) {
            return false;
        }

        return true;
    }


    public boolean LoadCursor() {
        if (cursor_ != -1) {
            return false; // loaded already
        }

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


    private int cursor_ = -1; // offset in bytes
    private String fileName_;
    private InputStream fileStream_;
    private String filesDir_;

    private final String[] sentenceDelimiters_ = {".", "...", ";"};
}
