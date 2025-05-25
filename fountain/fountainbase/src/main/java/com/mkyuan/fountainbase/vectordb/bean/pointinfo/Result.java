package com.mkyuan.fountainbase.vectordb.bean.pointinfo;

import java.io.Serializable;

public class Result implements Serializable {

	private int id = -1;
	private Payload payload = new Payload();

	public Payload getPayload() {
		return payload;
	}

	public void setPayload(Payload payload) {
		this.payload = payload;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

}
