package com.android.mangasanctuary.bd;

import java.util.Enumeration;
import java.util.Hashtable;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;

import com.android.mangasanctuary.datas.Global;
import com.android.mangasanctuary.datas.Serie;
import com.android.mangasanctuary.datas.Tome;
import com.android.mangasanctuary.http.ServerConnector;
import com.eightmotions.apis.tools.Log;

public class MyDbAdaptor {

    private MySQLiteOpenHelper baseHelper;
    private SQLiteDatabase     database;

    public MyDbAdaptor(Context context) {
        Log.d(Global.getLogTag(MyDbAdaptor.class), "create new Adaptor");
        baseHelper = new MySQLiteOpenHelper(context);
    }

    public void openToRead() throws android.database.SQLException {
        database = baseHelper.getReadableDatabase();
    }

    public void openToWrite() throws android.database.SQLException {
        database = baseHelper.getWritableDatabase();
    }

    public void close() {
        if (database != null) database.close();
        database = null;
    }

    public boolean insertSerie(Handler handler, Serie serie) {
        if (serie == null) return false;
        Hashtable<String, String> editions = serie.getEditions();
        Log.d(Global.getLogTag(MyDbAdaptor.class), "insertSerie "
            + serie.getName() + "(" + serie.getId() + ") - "
            + serie.getTomeCount());
        Enumeration<String> e = editions.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            String value = editions.get(key);
            Log.d(Global.getLogTag(MyDbAdaptor.class), "id_edition = " + key
                + " count=" + value);
        }

        boolean result = false;
        ContentValues values = new ContentValues();
        values.put(MySQLiteOpenHelper.COL_SERIES_ID, serie.getId());
        values.put(MySQLiteOpenHelper.COL_SERIES_NAME, serie.getName());

        // Find row
        Cursor cursor = database.query(MySQLiteOpenHelper.TABLE_SERIES, null, new StringBuilder().append(MySQLiteOpenHelper.COL_SERIES_ID).append("=?").toString(), new String[] { Integer.toString(serie.getId()) }, null, null, null);

        Enumeration<String> key = serie.getEditions().keys();
        int tome_count;
        String id_edition;
        if (cursor.getCount() == 0) {
            result = (database.insert(MySQLiteOpenHelper.TABLE_SERIES, null, values) >= 0);
            while (key.hasMoreElements()) {
                id_edition = key.nextElement();
                tome_count = Integer.parseInt(serie.getEditions().get(id_edition));
                if (tome_count != getTomesCountFromEditionId(id_edition)) {
                    // sync editions tomes
                    ServerConnector.syncEdition(handler, id_edition, serie.getId());
                }
            }
        }
        else {
            cursor.moveToFirst();
            int tomeCount = 0;
            if (serie.getTomeCount() != (tomeCount = getTomesCountFromSerieId(serie.getId()))) {
                while (key.hasMoreElements()) {
                    id_edition = key.nextElement();
                    tome_count = Integer.parseInt(serie.getEditions().get(id_edition));
                    if (tome_count != getTomesCountFromEditionId(id_edition)) {
                        // sync editions tomes
                        ServerConnector.syncEdition(handler, id_edition, serie.getId());
                    }
                }
            }
            else if (tomeCount > 0) checkTomeIcons(handler, serie.getId());

            Serie saved = loadSerieFromCursor(cursor);
            if (!saved.equals(serie))
                result = (database.update(MySQLiteOpenHelper.TABLE_SERIES, values, new StringBuilder().append(MySQLiteOpenHelper.COL_SERIES_ID).append("=?").toString(), new String[] { Integer.toString(serie.getId()) }) >= 0);
        }
        cursor.close();
        return result;
    }

    private void checkTomeIcons(Handler handler, int sid) {
        Cursor cursor = database.rawQuery(MySQLiteOpenHelper.SELECT_UNICON_TOME_FROM_SERIE_ID_RQST, new String[] { Integer.toString(sid) });
        if (cursor.moveToFirst()) {
            Tome tome;
            while (!cursor.isAfterLast()) {
                tome = new Tome();
                tome.setId(cursor.getInt(cursor.getColumnIndex(MySQLiteOpenHelper.COL_TOME_ID)));
                tome.setNumber(cursor.getInt(cursor.getColumnIndex(MySQLiteOpenHelper.COL_TOME_NUMBER)));
                tome.setEditionId(cursor.getInt(cursor.getColumnIndex(MySQLiteOpenHelper.COL_TOME_EDITION_ID)));
                tome.setIconUrl(cursor.getString(cursor.getColumnIndex(MySQLiteOpenHelper.COL_TOME_ICON_URL)));
                tome.setSerieId(sid);
                ServerConnector.getTomeIcon(handler, tome);
                cursor.moveToNext();
            }
        }
        cursor.close();
    }

    public Serie loadSerieFromCursor(Cursor cursor) {
        Serie serie = new Serie();
        int columnIndex;
        if ((columnIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_SERIES_ID)) >= 0)
            serie.setId(cursor.getInt(columnIndex));
        if ((columnIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_SERIES_NAME)) >= 0)
            serie.setName(cursor.getString(columnIndex));
        return serie;
    }

    public Cursor getAllSeries() {
        Cursor cursor = database.rawQuery(MySQLiteOpenHelper.SELECT_ALL_SERIES_RQST, null);
        return cursor;
    }

    public int getTomesCountFromSerieId(int serie_id) {
        Cursor cursor = database.rawQuery(MySQLiteOpenHelper.SELECT_COUNT_TOME_FROM_SERIE_ID_RQST, new String[] { Integer.toString(serie_id) });
        cursor.moveToFirst();
        int count = cursor.getInt(cursor.getColumnIndex("COUNT(*)"));
        cursor.close();
        return count;
    }

    public boolean insertTome(Handler handler, Tome tome) {
        if (tome == null) return false;
        Log.d(Global.getLogTag(MyDbAdaptor.class), "insertTome "
            + tome.getNumber() + "(" + tome.getId() + ") - "
            + tome.getSerieId() + " url=" + tome.getIconUrl() + " icon="
            + tome.getIcon());

        boolean result = false;
        ContentValues values = new ContentValues();
        values.put(MySQLiteOpenHelper.COL_TOME_ID, tome.getId());
        values.put(MySQLiteOpenHelper.COL_TOME_SERIE_ID, tome.getSerieId());
        values.put(MySQLiteOpenHelper.COL_TOME_EDITION_ID, tome.getEditionId());
        values.put(MySQLiteOpenHelper.COL_TOME_NUMBER, tome.getNumber());
        values.put(MySQLiteOpenHelper.COL_TOME_ICON_URL, tome.getIconUrl());
        if (tome.getIconUrl() == null)
            values.putNull(MySQLiteOpenHelper.COL_TOME_ICON);

        // Find row
        Cursor cursor = database.query(MySQLiteOpenHelper.TABLE_TOMES, null, new StringBuilder().append(MySQLiteOpenHelper.COL_TOME_ID).append("=?").toString(), new String[] { Integer.toString(tome.getId()) }, null, null, null);

        if (cursor.getCount() == 0) {
            result = (database.insert(MySQLiteOpenHelper.TABLE_TOMES, null, values) >= 0);
            if (tome.getIconUrl() != null)
                ServerConnector.getTomeIcon(handler, tome);
        }
        else {
            cursor.moveToFirst();
            Tome saved = loadTomeFromCursor(cursor);
            Log.d(Global.getLogTag(MyDbAdaptor.class), "saved url="
                + saved.getIconUrl() + " icon=" + saved.getIcon());
            if (tome.getIconUrl() != null) {
                if (saved.getIconUrl() != null
                    && tome.getIconUrl().equals(saved.getIconUrl())
                    && saved.getIcon() != null) {
                    values.remove(MySQLiteOpenHelper.COL_TOME_ICON);
                }
                else {
                    ServerConnector.getTomeIcon(handler, tome);
                }
            }

            if (!tome.equals(saved))
                result = (database.update(MySQLiteOpenHelper.TABLE_TOMES, values, new StringBuilder().append(MySQLiteOpenHelper.COL_TOME_ID).append("=?").toString(), new String[] { Integer.toString(tome.getId()) }) >= 0);
        }
        cursor.close();
        return result;
    }

    public Tome loadTomeFromCursor(Cursor cursor) {
        Tome tome = new Tome();
        int columnIndex;
        if ((columnIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_TOME_ID)) >= 0)
            tome.setId(cursor.getInt(columnIndex));
        if ((columnIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_TOME_EDITION_ID)) >= 0)
            tome.setEditionId(cursor.getInt(columnIndex));
        if ((columnIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_TOME_SERIE_ID)) >= 0)
            tome.setSerieId(cursor.getInt(columnIndex));
        if ((columnIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_TOME_NUMBER)) >= 0)
            tome.setNumber(cursor.getInt(columnIndex));
        if ((columnIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_TOME_ICON_URL)) >= 0)
            tome.setIconUrl(cursor.getString(columnIndex));
        if ((columnIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_TOME_ICON)) >= 0)
            tome.setIcon(cursor.getBlob(columnIndex));
        return tome;
    }

    public int getTomesCountFromEditionId(String id_edition) {
        Cursor cursor = database.rawQuery(MySQLiteOpenHelper.SELECT_COUNT_TOME_FROM_EDITION_ID_RQST, new String[] { id_edition });
        cursor.moveToFirst();
        int count = cursor.getInt(cursor.getColumnIndex("COUNT(*)"));
        cursor.close();
        return count;
    }

    public Cursor getAllTomesFromSerieId(int serie_id) {
        Cursor cursor = database.rawQuery(MySQLiteOpenHelper.SELECT_TOME_FROM_SERIE_ID_RQST, new String[] { Integer.toString(serie_id) });
        Log.w(Global.getLogTag(MyDbAdaptor.class), "getAllTomesFromSerieId("
            + serie_id + ") -> " + cursor.getCount());
        return cursor;
    }

    public boolean updateTomeIcon(Tome tome) {
        if (tome == null) return false;
        Log.d(Global.getLogTag(MyDbAdaptor.class), "insert tome icon "
            + tome.getNumber() + "(" + tome.getId() + ") - "
            + tome.getSerieId());

        boolean result = false;
        ContentValues values = new ContentValues();
        values.put(MySQLiteOpenHelper.COL_TOME_ICON, tome.getIcon());

        // Find row
        Cursor cursor = database.query(MySQLiteOpenHelper.TABLE_TOMES, null, new StringBuilder().append(MySQLiteOpenHelper.COL_TOME_ID).append("=?").toString(), new String[] { Integer.toString(tome.getId()) }, null, null, null);

        if (cursor.getCount() == 0) {
            return false;
        }
        else {
            cursor.moveToFirst();
            result = (database.update(MySQLiteOpenHelper.TABLE_TOMES, values, new StringBuilder().append(MySQLiteOpenHelper.COL_TOME_ID).append("=?").toString(), new String[] { Integer.toString(tome.getId()) }) >= 0);
        }
        cursor.close();
        return result;
    }
}
