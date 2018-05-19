package backend;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import utils.CountingInputStream;


/**
 * Created by as.grebennikov on 15.01.18.
 */

public final class TextSupplierImpl implements TextSupplier {

    public TextSupplierImpl(String filesDir,
                            InputStream fileStream,
                            String fileName) throws IOException {
        fileName_ = fileName;
        fileStream_ = new CountingInputStream(fileStream);
        fileStream_.mark(2048);
        filesDir_ = filesDir;
        scanner_ = new Scanner(fileStream_).useDelimiter("(?<=[.?!;])\\s+(?=\\p{Lu})");

    }

    public Sentence GetNextSentence() {
        String sentenceStr = scanner_.hasNext() ? scanner_.next() : "";
        cursor_ += sentenceStr.getBytes().length + 1 /*(+whitespace = 1 byte)*/;
        sentenceStr = sentenceStr.replaceAll("(\\r|\\n)", " ");
        return new SentenceImpl(sentenceStr);
    }

    public Sentence GetPrevSentence() {
        return null;
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


    private long cursor_ = 0; // offset in bytes
    private String fileName_;
    private CountingInputStream fileStream_;
    private String filesDir_;
    private Scanner scanner_;

    private final String[] sentenceDelimiters_ = {".", "...", ";"};
}
