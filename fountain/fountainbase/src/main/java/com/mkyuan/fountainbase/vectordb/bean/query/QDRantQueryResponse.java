package com.mkyuan.fountainbase.vectordb.bean.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class QDRantQueryResponse implements Serializable {
	private List<QDRantQueryResult> result = new ArrayList<QDRantQueryResult>();
	private String status = "";

	public String getStatus() {
		return status;
	}

	public List<QDRantQueryResult> getResult() {
		return result;
	}

	public void setResult(List<QDRantQueryResult> result) {
		this.result = new ArrayList<>(result);
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
	}

	private double time;
}
