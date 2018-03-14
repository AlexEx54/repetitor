package backend;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
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
            TranslationResult translationResult = ParseWordArticleXml(stream);
        } catch (Exception e) {
            return new Vector<Word>();
        }

        return result;
    }


    private InputStream GetWordArticleXmlStream(Word word, TranslateDirection direction) throws IOException {
        String translateDirectionRequestPart = (direction == TranslateDirection.AUTO) ? ""
                : "lang=" + ToString(direction) + "&";

        URL url = new URL("https://dictionary.yandex.net/api/v1/dicservice/lookup?" +
                "key=" + yandexKey + "&" +
                translateDirectionRequestPart +
                "flags=4" + "&" +
                "text=" + word.GetText());
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        InputStream stream = new BufferedInputStream(urlConnection.getInputStream());

        return stream;
    }


    private TranslationResult ParseWordArticleXml(InputStream stream) throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(stream, null);
        parser.next();

        TranslationResult result = new TranslationResult();

        parser.require(XmlPullParser.START_TAG, null, "DicResult");

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("def")) {
                result.articles.add(ParseDef(parser));
            } else {
                skip(parser);
            }
        }

        return result;
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


    private TranslationResult ParseDicResult(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "head");

    }

    private static String yandexKey = "dict.1.1.20171219T092115Z.b4d251fe6793335a.ce9863aa4d1660707da362b3a1e2122fa7db659c";
}

class TranslationResult {
    public Vector<TranslateArticle> articles;
}

class TranslateArticle {
    String wordType;
    Vector<Translation> translations;
}

class Translation {
    String text;
    Vector<String> synonyms;
}
