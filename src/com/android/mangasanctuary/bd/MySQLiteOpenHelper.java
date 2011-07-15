package com.android.mangasanctuary.bd;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.android.mangasanctuary.datas.Global;
import com.eightmotions.apis.tools.Log;

public class MySQLiteOpenHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME                          = "MS.sqlite";
    private static final int    VERSION                                = 3;

    public static final String  TABLE_SERIES                           = "series";
    public static final String  COL_SERIES_ID                          = "id";
    public static final String  COL_SERIES_NAME                        = "name";

    private static final String CREATE_SERIES_TABLE                    = "CREATE TABLE "
                                                                           + TABLE_SERIES
                                                                           + " ("
                                                                           + COL_SERIES_ID
                                                                           + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                                                                           + COL_SERIES_NAME
                                                                           + " TEXT NOT NULL "
                                                                           + ");";

    public static final String  SELECT_ALL_SERIES_RQST                 = "SELECT "
                                                                           + COL_SERIES_ID
                                                                           + " AS _id, "
                                                                           + COL_SERIES_NAME
                                                                           + " FROM "
                                                                           + TABLE_SERIES
                                                                           + " ORDER BY "
                                                                           + COL_SERIES_NAME
                                                                           + " COLLATE LOCALIZED";

    public static final String  TABLE_TOMES                            = "tomes";
    public static final String  COL_TOME_ID                            = "id";
    public static final String  COL_TOME_NUMBER                        = "number";
    public static final String  COL_TOME_SERIE_ID                      = "serie_id";
    public static final String  COL_TOME_EDITION_ID                    = "edition_id";
    public static final String  COL_TOME_ICON                          = "icone";
    public static final String  COL_TOME_ICON_URL                      = "icone_url";

    private static final String CREATE_TOME_TABLE                      = "CREATE TABLE "
                                                                           + TABLE_TOMES
                                                                           + " ("
                                                                           + COL_TOME_ID
                                                                           + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                                                                           + COL_TOME_NUMBER
                                                                           + " INTEGER NOT NULL, "
                                                                           + COL_TOME_SERIE_ID
                                                                           + " INTEGER NOT NULL, "
                                                                           + COL_TOME_EDITION_ID
                                                                           + " INTEGER NOT NULL, "
                                                                           + COL_TOME_ICON
                                                                           + " BLOB, "
                                                                           + COL_TOME_ICON_URL
                                                                           + " TEXT, "
                                                                           + "FOREIGN KEY("
                                                                           + COL_TOME_SERIE_ID
                                                                           + ") REFERENCES "
                                                                           + TABLE_SERIES
                                                                           + "("
                                                                           + COL_SERIES_ID
                                                                           + ")"
                                                                           + ");";

    public static final String  SELECT_COUNT_TOME_FROM_SERIE_ID_RQST   = "SELECT COUNT(*) FROM "
                                                                           + TABLE_TOMES
                                                                           + " WHERE "
                                                                           + COL_TOME_SERIE_ID
                                                                           + "=?";

    public static final String  SELECT_COUNT_TOME_FROM_EDITION_ID_RQST = "SELECT COUNT(*) FROM "
                                                                           + TABLE_TOMES
                                                                           + " WHERE "
                                                                           + COL_TOME_EDITION_ID
                                                                           + "=?";

    public static final String  SELECT_TOME_FROM_SERIE_ID_RQST         = "SELECT "
                                                                           + COL_TOME_ID
                                                                           + " AS _id, "
                                                                           + COL_TOME_NUMBER
                                                                           + ", "
                                                                           + COL_TOME_ICON
                                                                           + " FROM "
                                                                           + TABLE_TOMES
                                                                           + " WHERE "
                                                                           + COL_TOME_SERIE_ID
                                                                           + "=?"
                                                                           + " ORDER BY "
                                                                           + COL_TOME_NUMBER;

    public static final String  SELECT_UNICON_TOME_FROM_SERIE_ID_RQST  = "SELECT *"
                                                                           + " FROM "
                                                                           + TABLE_TOMES
                                                                           + " WHERE "
                                                                           + COL_TOME_SERIE_ID
                                                                           + "=?"
                                                                           + " AND "
                                                                           + COL_TOME_ICON_URL
                                                                           + " IS NOT NULL"
                                                                           + " AND "
                                                                           + COL_TOME_ICON
                                                                           + " IS NULL"
                                                                           ;

    public MySQLiteOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
        Log.d(Global.getLogTag(MySQLiteOpenHelper.class), "create new Helper");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(Global.getLogTag(MySQLiteOpenHelper.class), "onCreateDb");
        //on créé la table à partir de la requête écrite dans la variable CREATE_TOME_TABLE
        db.execSQL(CREATE_SERIES_TABLE);
        db.execSQL(CREATE_TOME_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(Global.getLogTag(MySQLiteOpenHelper.class), "onUpgradeDb");
        //On peut fait ce qu'on veut ici moi j'ai décidé de supprimer la table et de la recréer
        //comme ça lorsque je change la version les id repartent de 0
        clean(db);
        onCreate(db);
    }

    public void clean(SQLiteDatabase db) {
        Log.d(Global.getLogTag(MySQLiteOpenHelper.class), "onCleanDb");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TOMES + ";");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SERIES + ";");
    }
}
