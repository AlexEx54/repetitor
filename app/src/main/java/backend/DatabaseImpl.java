package backend;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;


/**
 * Created by as.grebennikov on 28.03.18.
 */

public class DatabaseImpl implements Database {

    public void Open(String path, String version) {
        db_ = SQLiteDatabase.openOrCreateDatabase(path + "/repetitor.db", null);
        String dbVersion = GetDatabaseVersion();
        if (dbVersion == null || !dbVersion.equals(version))
        {
            CreateDatabase(version);
        }
    }

    public WordContext GetNextWord(@Nullable WordContext prevWord) {
        assert(db_ != null);
        assert(db_.isOpen());

        long prevWordTimestamp = (prevWord == null) ? 0 : prevWord.timestamp;

        try {
            Cursor sql_result = db_.rawQuery("SELECT * from learning_words WHERE timestamp > " +
                    Long.toString(prevWordTimestamp) + " LIMIT 1", null);

            sql_result.moveToFirst();

            long timestamp = sql_result.getLong(sql_result.getColumnIndexOrThrow("timestamp"));
            String word = sql_result.getString(sql_result.getColumnIndexOrThrow("word"));
            String containing_sentence =
                    sql_result.getString(sql_result.getColumnIndexOrThrow("containing_sentence"));
            String complementary_sentence =
                    sql_result.getString(sql_result.getColumnIndexOrThrow("complementary_sentence"));
            String translation_direction_str =
                    sql_result.getString(sql_result.getColumnIndexOrThrow("translation_direction"));

            WordContext result = new WordContext();
            result.timestamp = timestamp;
            result.word = new Word(word);
            result.containingSentence = new SentenceImpl(containing_sentence);
            result.complementarySentence = new SentenceImpl(complementary_sentence);
            result.translateDirection = Vocabulary.TranslateDirection.valueOf(translation_direction_str);

            return result;

        } catch (Exception e) {
            return null;
        }
    }

    public void SaveWord(WordContext wordContext) {
        assert(db_ != null);
        assert(db_.isOpen());

        ContentValues contentValues = new ContentValues();
        contentValues.put("timestamp", wordContext.timestamp);
        contentValues.put("word", wordContext.word.GetText());
        contentValues.put("containing_sentence", wordContext.containingSentence.AsString());
        contentValues.put("complementary_sentence", wordContext.complementarySentence.AsString());

        String s = wordContext.translateDirection.name();
        contentValues.put("translation_direction", wordContext.translateDirection.name());

        db_.replaceOrThrow("learning_words", null, contentValues);
    }

    public void RemoveWord(WordContext wordContext) {
        assert(db_ != null);
        assert(db_.isOpen());

        db_.delete("learning_words",
                "timestamp=? AND word=?",
                new String[] {Long.toString(wordContext.timestamp), wordContext.word.GetText()});
    }


    private String GetDatabaseVersion()
    {
        assert(db_ != null);
        assert(db_.isOpen());

        try {
            Cursor sql_result = db_.rawQuery("SELECT version FROM db_info", null);
            sql_result.moveToFirst();
            int index_col = sql_result.getColumnIndexOrThrow("version");
            String version = sql_result.getString(index_col);
            return  version;
        }
        catch (Exception e) {
            return null;
        }
    }


    private void CreateDatabase(String version) {
        assert(db_ != null);
        assert(db_.isOpen());

        CreateVersionTable(version);
        CreateLearningWordsTable();
    }


    private void CreateVersionTable(String version) {
        db_.execSQL("CREATE TABLE db_info (version TEXT NOT NULL)");

        ContentValues contentValues = new ContentValues();
        contentValues.put("version", version);
        db_.insertOrThrow("db_info",null, contentValues);
    }


    private void CreateLearningWordsTable() {
        db_.execSQL("CREATE TABLE learning_words (" +
                    "timestamp INTEGER NOT NULL PRIMARY KEY," +
                    "word TEXT NOT NULL," +
                    "containing_sentence TEXT," +
                    "complementary_sentence TEXT," +
                    "translation_direction TEXT NOT NULL)");
    }

    private SQLiteDatabase db_ = null;
}
