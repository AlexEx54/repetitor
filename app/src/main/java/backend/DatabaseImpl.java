package backend;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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

    public void SaveWord(WordContext wordContext) {}

    public void RemoveWord(WordContext wordContext) {}


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
                    "id TEXT NOT NULL PRIMARY KEY," +
                    "word TEXT NOT NULL," +
                    "containing_sentence TEXT," +
                    "complementary_sentence TEXT," +
                    "translation_direction INT NOT NULL)");
    }

    private SQLiteDatabase db_ = null;
}
