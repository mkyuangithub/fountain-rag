package com.mkyuan.fountainbase.vectordb.bean.addpoint;

import java.io.Serializable;

public class QDRantResponse implements Serializable {
	private Result result = new Result();
	private String status = "";

	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}

	public String getStatus() {
		return status;
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
