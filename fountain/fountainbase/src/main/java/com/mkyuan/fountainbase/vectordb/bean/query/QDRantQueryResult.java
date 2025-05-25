package com.mkyuan.fountainbase.vectordb.bean.query;

import java.io.Serializable;
import java.util.Objects;

import com.mkyuan.fountainbase.vectordb.bean.pointinfo.Payload;

public class QDRantQueryResult implements Serializable {
    private int operation_id = -1;
    private String status = "";
    private long id = -1;
    private int version = -1;
    private float score = -1.0f;

    private Payload payload;

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public int getOperation_id() {
        return operation_id;
    }

    public void setOperation_id(int operation_id) {
        this.operation_id = operation_id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        QDRantQueryResult that = (QDRantQueryResult) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
