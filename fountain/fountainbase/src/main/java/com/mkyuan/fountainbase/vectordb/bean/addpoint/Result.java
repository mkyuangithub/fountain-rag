package com.mkyuan.fountainbase.vectordb.bean.addpoint;

import java.io.Serializable;

public class Result implements Serializable {
	private int operation_id = -1;
	private String status = "";

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
}
