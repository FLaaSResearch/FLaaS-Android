package org.sensingkit.flaas.model;

import com.google.gson.annotations.SerializedName;

public class RetroToken {

    @SerializedName("refresh")
    private String refresh;

    @SerializedName("access")
    private String access;

    public RetroToken(String refresh, String access) {
        this.refresh = refresh;
        this.access = access;
    }

    public String getAccess() {
        return access;
    }

    public void setAccess(String access) {
        this.access = access;
    }

    public String getRefresh() {
        return refresh;
    }

    public void setRefresh(String refresh) {
        this.refresh = refresh;
    }
}
