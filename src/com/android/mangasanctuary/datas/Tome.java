package com.android.mangasanctuary.datas;

import com.eightmotions.apis.tools.arrays.ArraysUtils;

public class Tome {
    // TABLE VARIABLES
    int    msSerieId;
    int    msEditionId;
    int    msId;
    int    msNumber;
    byte[] msIcon;
    String msIconUrl;

    // READ VARIABLES

    public Tome() {
    }

    public int getSerieId() {
        return msSerieId;
    }

    public void setSerieId(int sid) {
        this.msSerieId = sid;
    }

    public int getId() {
        return msId;
    }

    public void setId(int id) {
        this.msId = id;
    }

    public int getNumber() {
        return msNumber;
    }

    public void setNumber(int number) {
        this.msNumber = number;
    }

    public byte[] getIcon() {
        return msIcon;
    }

    public void setIcon(byte[] icon) {
        this.msIcon = icon;
    }

    public String getIconUrl() {
        return msIconUrl;
    }

    public void setIconUrl(String url) {
        this.msIconUrl = url;
    }

    public int getEditionId() {
        return msEditionId;
    }

    public void setEditionId(int eid) {
        this.msEditionId = eid;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Tome)) return false;

        if (this.msId != ((Tome) o).msId) return false;
        if (this.msSerieId != ((Tome) o).msSerieId) return false;
        if (this.msEditionId != ((Tome) o).msEditionId) return false;
        if (this.msNumber != ((Tome) o).msNumber) return false;
        if ((this.msIconUrl == null && ((Tome) o).msIconUrl != null)
            || (this.msIconUrl != null && !this.msIconUrl.equals(((Tome) o).msIconUrl)))
            return false;
        if (!ArraysUtils.equals(this.msIcon, ((Tome) o).msIcon)) return false;

        return true;
    }

}
