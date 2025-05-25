package com.mkyuan.fountainbase.vectordb.bean.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Filter implements Serializable {
	private List<Match> should = new ArrayList<Match>();
	public List<Match> must_not=new ArrayList<>();
	public List<Match> getShould() {
		return should;
	}

	public void setShould(List<Match> should) {
		this.should = should;
	}

	public List<Match> getMust_not() {
		return must_not;
	}

	public void setMust_not(List<Match> must_not) {
		this.must_not = must_not;
	}

}
