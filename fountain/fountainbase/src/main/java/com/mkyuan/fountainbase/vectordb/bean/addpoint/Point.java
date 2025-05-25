package com.mkyuan.fountainbase.vectordb.bean.addpoint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Point implements Serializable {
	private long id = -1;
	private List<Float> vector = new ArrayList<Float>();
	private Payload payload;



	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public List<Float> getVector() {
		return vector;
	}

	public void setVector(List<Float> vector) {
		this.vector = vector;
	}

	public Payload getPayload() {
		return payload;
	}

	public void setPayload(Payload payload) {
		this.payload = payload;
	}
}
