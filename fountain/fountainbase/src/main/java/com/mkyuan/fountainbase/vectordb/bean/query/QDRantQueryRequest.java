package com.mkyuan.fountainbase.vectordb.bean.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class QDRantQueryRequest implements Serializable {
	private Filter filter = new Filter();
	private List<Float> vector = new ArrayList<Float>();

	boolean with_payload=false;

	public boolean isWith_payload() {
		return with_payload;
	}

	public void setWith_payload(boolean with_payload) {
		this.with_payload = with_payload;
	}

	private int top = 3;

	public Filter getFilter() {
		return filter;
	}

	public void setFilter(Filter filter) {
		this.filter = filter;
	}

	public List<Float> getVector() {
		return vector;
	}

	public void setVector(List<Float> vector) {
		this.vector = vector;
	}

	public int getTop() {
		return top;
	}

	public void setTop(int top) {
		this.top = top;
	}
}
