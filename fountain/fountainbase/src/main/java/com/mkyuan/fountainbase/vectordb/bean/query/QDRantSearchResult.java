package com.mkyuan.fountainbase.vectordb.bean.query;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class QDRantSearchResult implements Serializable {

	private static final long serialVersionUID = 1L;

	private long id = -1;
	private int version = -1;
	private float score = -1.0f;
	private Map<String, Object> payload;
	private List<Float> vector;
	
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
	public Map<String, Object> getPayload() {
		return payload;
	}
	public void setPayload(Map<String, Object> payload) {
		this.payload = payload;
	}
	public List<Float> getVector() {
		return vector;
	}
	public void setVector(List<Float> vector) {
		this.vector = vector;
	}
	
}