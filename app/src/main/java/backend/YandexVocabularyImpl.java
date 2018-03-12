package backend;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import java.util.Vector;

/**
 * Created by as.grebennikov on 21.02.18.
 */

public class YandexVocabularyImpl implements Vocabulary {

    public Vector<Word> Translate(Word word, TranslateDirection direction) {
        Vector<Word> result = new Vector<Word>();

        try {
            InputStream stream = GetWordArticleXmlStream(word, direction);

            Scanner s = new Scanner(stream).useDelimiter("\\A");
            String string = s.hasNext() ? s.next() : "";
            string = "";
        } catch (IOException e) {
            return new Vector<Word>();
        }

        return result;
    }


    private InputStream GetWordArticleXmlStream(Word word, TranslateDirection direction) throws IOException {
        String translateDirectionRequestPart = (direction == TranslateDirection.AUTO) ? ""
                : "lang=" + ToString(direction) + "&";

        URL url = new URL("https://dictionary.yandex.net/api/v1/dicservice.json/lookup?" +
                "key=" + yandexKey + "&" +
                translateDirectionRequestPart +
                "flags=4" + "&" +
                "text=" + word.GetText());
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        InputStream stream = new BufferedInputStream(urlConnection.getInputStream());

        return stream;
    }


    private String ToString(TranslateDirection direction) {
        switch (direction) {
            case RU_EN:
                return "ru-en";
            case EN_RU:
                return "en-ru";
        }

        return  "";
    }

    private static String yandexKey = "dict.1.1.20171219T092115Z.b4d251fe6793335a.ce9863aa4d1660707da362b3a1e2122fa7db659c";
}