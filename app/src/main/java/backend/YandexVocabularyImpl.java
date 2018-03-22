package backend;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

/**
 * Created by as.grebennikov on 21.02.18.
 */

public class YandexVocabularyImpl implements Vocabulary {

    public Vector<Word> Translate(Word word, TranslateDirection direction) {
        Vector<Word> result = null;

        try {
            InputStream stream = GetWordArticleXmlStream(word, direction);
            TranslationResult translationResult = ParseWordArticleXml(stream, word);
            result = TranslationResultAsList(translationResult);
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


    private TranslationResult ParseWordArticleXml(InputStream stream, Word word) throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(stream, null);
        parser.next();

        TranslationResult result = new TranslationResult();
        result.articles = new Vector<TranslateArticle>();
        result.request = word;

        parser.require(XmlPullParser.START_TAG, null, "DicResult");

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("def")) {
                result.articles.add(ParseDef(parser));
            } else {
                Skip(parser);
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


    private TranslateArticle ParseDef(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "def");

        TranslateArticle result = new TranslateArticle();
        result.translations = new Vector<Translation>();
        result.wordType = parser.getAttributeValue(null, "pos");

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tag_name = parser.getName();
            if (tag_name.equals("tr")) {
                result.translations.add(ParseTr(parser));
            } else {
                Skip(parser);
            }
        }

        return result;
    }


    private Translation ParseTr(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "tr");

        Translation result = new Translation();
        result.synonyms = new Vector<String>();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tag_name = parser.getName();

            if (tag_name.equals("text")) {
                result.text = ReadText(parser);
                parser.require(XmlPullParser.END_TAG, null, "text");
                continue;
            }

            if (tag_name.equals("syn")) {
                result.synonyms.add(ParseSyn(parser));
            } else {
                Skip(parser);
            }
        }

        return result;
    }


    private String ParseSyn(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "syn");

        String result = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String tag_name = parser.getName();

            if (tag_name.equals("text")) {
                result = ReadText(parser);
                parser.require(XmlPullParser.END_TAG, null, "text");
            } else {
                Skip(parser);
            }
        }

        if (result == null) {
            throw new XmlPullParserException("There's no 'text' tag inside 'syn'!!");
        }

        return result;
    }


    private void Skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }


    private String ReadText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }


    private Vector<Word> TranslationResultAsList(TranslationResult translation_result) {
        Vector<Word> result = new Vector<Word>();

        Vector<Word> translations_only = new Vector<Word>();
        Vector<Word> synonyms = new Vector<Word>();

        for (TranslateArticle article: translation_result.articles) {
            for (Translation translation: article.translations) {
                translations_only.add(new Word(translation.text));
                for (String synonym: translation.synonyms) {
                    synonyms.add(new Word(synonym));
                }
            }
        }

        result.addAll(translations_only);
        result.addAll(synonyms);

        return result;
    }


    private static String yandexKey = "dict.1.1.20171219T092115Z.b4d251fe6793335a.ce9863aa4d1660707da362b3a1e2122fa7db659c";
}

class TranslationResult {
    public Word request;
    public Vector<TranslateArticle> articles;
}

class TranslateArticle {
    public String wordType;
    public Vector<Translation> translations;
}

class Translation {
    public String text;
    public Vector<String> synonyms;
}
