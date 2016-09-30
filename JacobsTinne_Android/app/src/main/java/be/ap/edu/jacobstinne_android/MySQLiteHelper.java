package be.ap.edu.jacobstinne_android;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tinne on 30/09/2016.
 */

public class MySQLiteHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "bibliotheek.db";
    private static final String TABLE_BIB = "bibliotheken";
    private static final int DATABASE_VERSION = 5;

    public MySQLiteHelper(Context context) {

        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_BIB + "(naam TEXT, point_lat DOUBLE, point_lng DOUBLE)";
        db.execSQL(CREATE_CONTACTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BIB);
        onCreate(db);
    }

    public void addBib(String naam, Double point_lat, Double point_lng) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("naam", naam);
        values.put("point_lat", point_lat);
        values.put("point_lng", point_lng);

        db.insert(TABLE_BIB, null, values);
        db.close();
    }


    public List<String> getAll() {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_BIB, new String[] { "*" }, null, null, null, null, null);

        List<String> list = new ArrayList<>();

        if(cursor != null)
            if (cursor.moveToFirst()) {
                do {
                    list.add(cursor.getString(0) + "," + cursor.getDouble(1) + "," + cursor.getDouble(2));
                } while (cursor.moveToNext());
            };

        return list;
    }
}
