package com.android.mangasanctuary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.android.mangasanctuary.bd.MySQLiteOpenHelper;
import com.android.mangasanctuary.datas.Global;
import com.eightmotions.apis.tools.Log;
import com.eightmotions.apis.tools.strings.StringUtils;

public class ShoppingActivity extends ListActivity {

    SeriesAdapter              cursorAdapter    = null;
    Cursor                     serieCursor      = null;
    ProgressDialog             waitingDialog    = null;

    Hashtable<Integer, Cursor> editionCursorMap = new Hashtable<Integer, Cursor>();

    private static class SeriesViewHolder {
        public TextView        separator;
        public TextView        title;
        public Gallery         tomes;
        public CharArrayBuffer titleBuffer = new CharArrayBuffer(128);
    }

    private static class TomesViewHolder {
        public ImageView icon;
        public TextView  title;
    }

    private class SeriesAdapter extends CursorAdapter implements SectionIndexer {

        /**
         * State of ListView item that has never been determined.
         */
        private static final int      STATE_UNKNOWN        = 0;

        /**
         * State of a ListView item that is sectioned. A sectioned item must
         * display the separator.
         */
        private static final int      STATE_SECTIONED_CELL = 1;

        /**
         * State of a ListView item that is not sectioned and therefore does not
         * display the separator.
         */
        private static final int      STATE_REGULAR_CELL   = 2;

        private final CharArrayBuffer mBuffer              = new CharArrayBuffer(128);
        private int[]                 mCellStates;
        private int                   mColumnNameIndex, mColumnIdIndex;

        HashMap<String, Integer>      alphaIndexer         = new HashMap<String, Integer>();
        String[]                      sections;

        public SeriesAdapter(Context context, Cursor cursor) {
            super(context, cursor);
            initAdapter(cursor);
        }

        public void initAdapter(Cursor cursor) {
            Log.w(Global.getLogTag(ShoppingActivity.class), "SeriesAdapter initAdapter");
            mCellStates = cursor == null ? null : new int[cursor.getCount()];
            mColumnNameIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_SERIES_NAME);
            mColumnIdIndex = cursor.getColumnIndex("_id");

            alphaIndexer.clear();
            String section;
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    cursor.copyStringToBuffer(mColumnNameIndex, mBuffer);
                    section = getSectionHeader(mBuffer.data[0]);
                    if (!alphaIndexer.containsKey(section))
                        alphaIndexer.put(section, cursor.getPosition());
                    cursor.moveToNext();
                }
            }
            cursor.moveToFirst();
            ArrayList<String> array = new ArrayList<String>(alphaIndexer.keySet());
            Collections.sort(array);
            sections = new String[array.size()];
            for (int i = sections.length; --i >= 0;)
                sections[i] = array.get(i);
        }

        @Override
        public void changeCursor(Cursor cursor) {
            Log.w(Global.getLogTag(ShoppingActivity.class), "SeriesAdapter changeCursor");
            super.changeCursor(cursor);
            initAdapter(cursor);
        }

        @Override
        public void notifyDataSetChanged() {
            Log.w(Global.getLogTag(ShoppingActivity.class), "SeriesAdapter notifyDataSetChanged");
            super.notifyDataSetChanged();
            initAdapter(getCursor());
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final SeriesViewHolder holder = (SeriesViewHolder) view.getTag();

            /*
             * Separator
             */
            boolean needSeparator = false;

            final int position = cursor.getPosition();

            if (mCellStates == null || mCellStates.length <= position) return;

            cursor.copyStringToBuffer(mColumnNameIndex, holder.titleBuffer);

            switch (mCellStates[position]) {
                case STATE_SECTIONED_CELL:
                    needSeparator = true;
                    break;

                case STATE_REGULAR_CELL:
                    needSeparator = false;
                    break;

                case STATE_UNKNOWN:
                default:
                    // A separator is needed if it's the first itemview of the
                    // ListView or if the group of the current cell is different
                    // from the previous itemview.
                    if (position == 0) {
                        needSeparator = true;
                    }
                    else {
                        cursor.moveToPosition(position - 1);

                        cursor.copyStringToBuffer(mColumnNameIndex, mBuffer);
                        if (mBuffer.sizeCopied > 0
                            && holder.titleBuffer.sizeCopied > 0
                            && !isSameSection(mBuffer.data[0], holder.titleBuffer.data[0])) {
                            needSeparator = true;
                        }

                        cursor.moveToPosition(position);
                    }

                    // Cache the result
                    mCellStates[position] = needSeparator ? STATE_SECTIONED_CELL : STATE_REGULAR_CELL;
                    break;
            }

            if (needSeparator) {
                //                char header = holder.titleBuffer.data[0];
                String sectionHeader = getSectionHeader(holder.titleBuffer.data[0]);
                holder.separator.setText(sectionHeader);
                holder.separator.setVisibility(View.VISIBLE);
            }
            else {
                holder.separator.setVisibility(View.GONE);
            }

            holder.title.setText(holder.titleBuffer.data, 0, holder.titleBuffer.sizeCopied);
            holder.title.setTextColor(Global.getResources().getColor(R.color.uptodateItemText));

            int serie_id = cursor.getInt(mColumnIdIndex);
            Cursor c = Global.getAdaptor().getAllMissingTomesFromSerieId(serie_id);
            startManagingCursor(c);
            holder.tomes.setAdapter(new MissingTomesAdapter(context, c));
            holder.tomes.setSelection(holder.tomes.getCount() - 1);
            editionCursorMap.put(serie_id, c);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {

            View v = LayoutInflater.from(context).inflate(R.layout.serie_item_layout, parent, false);

            // The following code allows us to keep a reference on the child
            // views of the item. It prevents us from calling findViewById at
            // each getView/bindView and boosts the rendering code.
            SeriesViewHolder holder = new SeriesViewHolder();
            holder.separator = (TextView) v.findViewById(R.id.separator);
            holder.title = (TextView) v.findViewById(R.id.serie_name);
            holder.tomes = (Gallery) v.findViewById(R.id.tome_gallery);

            v.setTag(holder);
            return v;
        }

        private boolean isSameSection(char prev, char current) {
            String prevHeader = getSectionHeader(prev);
            String currentHeader = getSectionHeader(current);
            return prevHeader.equals(currentHeader);
        }

        private String getSectionHeader(char letter) {
            String header = String.valueOf(StringUtils.getUnaccentedChar(letter)).toUpperCase();
            if (letter >= 0x30 && letter <= 0x39) header = "0-9";
            return header;
        }

        @Override
        public int getPositionForSection(int section) {
            if (section < sections.length) {
                Log.d(Global.getLogTag(SeriesAdapter.class), "getPositionForSection("
                    + section + ")=" + alphaIndexer.get(sections[section]));
                return alphaIndexer.get(sections[section]);
            }
            Log.d(Global.getLogTag(SeriesAdapter.class), "getPositionForSection("
                + section + ")=0");
            return 0;
            //            return alphaIndexer.getPositionForSection(section);
        }

        @Override
        public int getSectionForPosition(int position) {
            int section = 0;
            while (section < sections.length) {
                if (alphaIndexer.get(sections[section]) > position) {
                    Log.d(Global.getLogTag(SeriesAdapter.class), "getSectionForPosition("
                        + position
                        + ")="
                        + ((section - 1 < 0) ? 0 : section - 1));
                    return ((section - 1 < 0) ? 0 : section - 1);
                }
                section++;
            }
            Log.d(Global.getLogTag(SeriesAdapter.class), "getSectionForPosition("
                + position + ")=0");
            return 0;
            //            return alphaIndexer.getSectionForPosition(position);
        }

        @Override
        public Object[] getSections() {
            for (int i = 0; i < sections.length; i++)
                Log.w(Global.getLogTag(SeriesAdapter.class), "\tsections[" + i
                    + "]=" + sections[i]);

            return sections;
            //            return alphaIndexer.getSections();
        }
    }

    private class MissingTomesAdapter extends SimpleCursorAdapter {

        private int mColumnNameIndex, mColumnIconIndex;

        public MissingTomesAdapter(Context context, Cursor cursor) {
            super(context, R.layout.tome_item_layout, cursor, new String[] { MySQLiteOpenHelper.COL_MISSING_ICON, MySQLiteOpenHelper.COL_MISSING_NUMBER }, new int[] { R.id.tome_icon, R.id.tome_name });
            initAdapter(cursor);
        }

        public void initAdapter(Cursor cursor) {
            Log.w(Global.getLogTag(ShoppingActivity.class), "MissingTomesAdapter initAdapter");
            mColumnNameIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_MISSING_NUMBER);
            mColumnIconIndex = cursor.getColumnIndex(MySQLiteOpenHelper.COL_MISSING_ICON);
        }

        @Override
        public void changeCursor(Cursor cursor) {
            Log.w(Global.getLogTag(ShoppingActivity.class), "MissingTomesAdapter changeCursor");
            super.changeCursor(cursor);
            initAdapter(cursor);
        }

        @Override
        public void notifyDataSetChanged() {
            Log.w(Global.getLogTag(ShoppingActivity.class), "MissingTomesAdapter notifyDataSetChanged");
            super.notifyDataSetChanged();
            initAdapter(getCursor());
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final TomesViewHolder holder = (TomesViewHolder) view.getTag();
            holder.title.setText(context.getResources().getString(R.string.Tome_Title_Pattern, cursor.getInt(mColumnNameIndex)));
            byte[] datas = cursor.getBlob(mColumnIconIndex);
            if (datas == null)
                holder.icon.setImageResource(R.drawable.no_icon);
            else {
                Bitmap icon = BitmapFactory.decodeByteArray(datas, 0, datas.length);
                holder.icon.setImageBitmap(icon);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = LayoutInflater.from(context).inflate(R.layout.tome_item_layout, parent, false);

            // The following code allows us to keep a reference on the child
            // views of the item. It prevents us from calling findViewById at
            // each getView/bindView and boosts the rendering code.
            TomesViewHolder holder = new TomesViewHolder();
            holder.icon = (ImageView) v.findViewById(R.id.tome_icon);
            holder.title = (TextView) v.findViewById(R.id.tome_name);

            v.setTag(holder);
            return v;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.series_list_layout);
        ListView myListView = (ListView) findViewById(android.R.id.list);
        myListView.setFastScrollEnabled(true);

        Global.getAdaptor().openToWrite();
        serieCursor = Global.getAdaptor().getAllMissingSeries();
        startManagingCursor(serieCursor);
        cursorAdapter = new SeriesAdapter(this, serieCursor);
        setListAdapter(cursorAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopManagingCursor(serieCursor);
        serieCursor = null;
        Enumeration<Integer> e = editionCursorMap.keys();
        while (e.hasMoreElements())
            stopManagingCursor(editionCursorMap.get(e.nextElement()));
    }
}
