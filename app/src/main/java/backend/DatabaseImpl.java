package backend;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by as.grebennikov on 28.03.18.
 */

public class DatabaseImpl implements Database {

    public void Open(String path, String version) {
        db_ = SQLiteDatabase.openOrCreateDatabase(path + "/repetitor.db", null);
        String dbVersion = GetDatabaseVersion();
        if (dbVersion == null || !dbVersion.equals(version))
        {
            ClearDatabase();
            CreateDatabase(version);
        }
    }

    public WordContext GetNextWord(@Nullable WordContext prevWord) {
        assert(db_ != null);
        assert(db_.isOpen());

        long prevWordTimestamp = (prevWord == null) ? 0 : prevWord.timestamp;

        try {
            Cursor sql_result = db_.rawQuery("SELECT * from learning_words WHERE timestamp > " +
                    Long.toString(prevWordTimestamp) + " ORDER BY timestamp LIMIT 1", null);

            sql_result.moveToFirst();

            long timestamp = sql_result.getLong(sql_result.getColumnIndexOrThrow("timestamp"));
            String word = sql_result.getString(sql_result.getColumnIndexOrThrow("word"));
            String primary_sentence_file_id =
                    sql_result.getString(sql_result.getColumnIndexOrThrow("primary_sentence_file_id"));
            long primary_sentence_cursor_pos =
                    sql_result.getLong(sql_result.getColumnIndexOrThrow("primary_sentence_cursor_pos"));
            String complementary_sentence_file_id =
                    sql_result.getString(sql_result.getColumnIndexOrThrow("complementary_sentence_file_id"));
            long complementary_sentence_cursor_pos =
                    sql_result.getLong(sql_result.getColumnIndexOrThrow("complementary_sentence_cursor_pos"));
            String translation_direction_str =
                    sql_result.getString(sql_result.getColumnIndexOrThrow("translation_direction"));

            WordContext result = new WordContext();
            result.timestamp = timestamp;
            result.word = new Word(word);
            result.primarySentenceFileId = primary_sentence_file_id;
            result.primarySentenceCursorPos = primary_sentence_cursor_pos;
            result.complementarySentenceFileId = complementary_sentence_file_id;
            result.complementarySentenceCursorPos = complementary_sentence_cursor_pos;
            result.translateDirection = Vocabulary.TranslateDirection.valueOf(translation_direction_str);

            return result;

        } catch (Exception e) {
            return null;
        }
    }

    public void SaveWord(WordContext wordContext) throws Exception {
        assert(db_ != null);
        assert(db_.isOpen());

        if (wordContext.word == null) {
            throw new Exception("SaveWord: passed null word to save");
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put("timestamp", wordContext.timestamp);
        contentValues.put("word", wordContext.word.GetText());
        contentValues.put("primary_sentence_file_id", wordContext.primarySentenceFileId);
        contentValues.put("primary_sentence_cursor_pos", wordContext.primarySentenceCursorPos);
        contentValues.put("complementary_sentence_file_id", wordContext.complementarySentenceFileId);
        contentValues.put("complementary_sentence_cursor_pos", wordContext.complementarySentenceCursorPos);
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


    public void SaveRewindPoint(String fileId, long cursorPos) {
        assert(db_ != null);
        assert(db_.isOpen());

        ContentValues contentValues = new ContentValues();
        contentValues.put("file_id", fileId);
        contentValues.put("cursor_pos", cursorPos);

        db_.replaceOrThrow("rewind_points", null, contentValues);
    }


    public long GetRewindPointBefore(String fileId, long beforePos) {
        Cursor sql_result = db_.rawQuery("SELECT cursor_pos FROM rewind_points WHERE cursor_pos < " +
                Long.toString(beforePos) + " AND file_id = '" + fileId +
                "' ORDER BY cursor_pos DESC LIMIT 1", null);

        sql_result.moveToFirst();

        long result = sql_result.getLong(sql_result.getColumnIndexOrThrow("cursor_pos"));

        return result;
    }

    @Override
    public boolean IsShowedTooltip(String componentId, String tooltipId) {
        assert(db_ != null);
        assert(db_.isOpen());

        Cursor sql_result = db_.rawQuery("SELECT tooltip_id FROM showed_tooltips WHERE component_id = '" +
                componentId + "' AND tooltip_id = '" + tooltipId +
                "' LIMIT 1", null);

        return (sql_result.getCount() != 0);
    }


    @Override
    public void SetTooltipAsShowed(String componentId, String tooltipId) {
        assert(db_ != null);
        assert(db_.isOpen());

        ContentValues contentValues = new ContentValues();
        contentValues.put("component_id", componentId);
        contentValues.put("tooltip_id", tooltipId);

        db_.replaceOrThrow("showed_tooltips", null, contentValues);
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
        CreateRewindPointsTable();
        CreateShowedTooltipsTable();
    }


    private void ClearDatabase() {
        Cursor c = db_.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        List<String> tables = new ArrayList<>();

        while (c.moveToNext()) {
            tables.add(c.getString(0));
        }

        for (String table : tables) {
            String dropQuery = "DROP TABLE IF EXISTS " + table;
            db_.execSQL(dropQuery);
        }
    }


    private void CreateVersionTable(String version) {
        db_.execSQL("CREATE TABLE db_info (version TEXT NOT NULL)");

        ContentValues contentValues = new ContentValues();
        contentValues.put("version", version);
        db_.insertOrThrow("db_info",null, contentValues);
    }


    private void CreateLearningWordsTable() {
        db_.execSQL("CREATE TABLE learning_words (" +
                    "timestamp INTEGER NOT NULL," +
                    "word TEXT NOT NULL," +
                    "primary_sentence_file_id TEXT," +
                    "primary_sentence_cursor_pos BIGINT," +
                    "complementary_sentence_file_id TEXT," +
                    "complementary_sentence_cursor_pos BIGINT," +
                    "translation_direction TEXT NOT NULL," +
                    "PRIMARY KEY(word, primary_sentence_file_id, primary_sentence_cursor_pos));");
    }


    private void CreateRewindPointsTable() {
        db_.execSQL("CREATE TABLE rewind_points (" +
                    "file_id TEXT NOT NULL," +
                    "cursor_pos INTEGER NOT NULL," +
                    "PRIMARY KEY(file_id, cursor_pos));");

        db_.execSQL("CREATE INDEX IF NOT EXISTS rewind_cursor_idx ON rewind_points (cursor_pos);");
    }


    private void CreateShowedTooltipsTable() {
        db_.execSQL("CREATE TABLE showed_tooltips (" +
                    "component_id TEXT NOT NULL," +
                    "tooltip_id TEXT NOT NULL," +
                    "PRIMARY KEY(component_id, tooltip_id));");
    }

    private SQLiteDatabase db_ = null;
}