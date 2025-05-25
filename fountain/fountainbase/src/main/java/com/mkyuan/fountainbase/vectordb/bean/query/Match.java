package com.mkyuan.fountainbase.vectordb.bean.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Match implements Serializable {



    private String key = "";

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public QDRantQueryValue getMatch() {
        return match;
    }

    public void setMatch(QDRantQueryValue match) {
        this.match = match;
    }

    private QDRantQueryValue match = new QDRantQueryValue();

}
