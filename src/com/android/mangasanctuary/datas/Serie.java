package com.android.mangasanctuary.datas;

import java.util.Hashtable;

public class Serie {
    // TABLE VARIABLES
    int    msId;
    String msName;

    public Serie() {
    }

    public void setId(int id) {
        msId = id;
    }

    public void setName(String name) {
        msName = name;
    }

    public int getId() {
        return msId;
    }

    public String getName() {
        return msName;
    }

    // READ VARIABLES
    int                       msTomeCount;
    Hashtable<String, String> msEditions = new Hashtable<String, String>();

    public void setTomeCount(int count) {
        msTomeCount = count;
    }

    public int getTomeCount() {
        return msTomeCount;
    }

    public void addEdition(int eid, int count) {
        msEditions.put(Integer.toString(eid), Integer.toString(count));
    }

    public Hashtable<String, String> getEditions() {
        return msEditions;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Serie)) return false;

        if (this.msId != ((Serie) o).msId) return false;
        if ((this.msName == null && ((Serie) o).msName != null)
            || (this.msName != null && !this.msName.equals(((Serie) o).msName)))
            return false;

        return true;
    }
}
