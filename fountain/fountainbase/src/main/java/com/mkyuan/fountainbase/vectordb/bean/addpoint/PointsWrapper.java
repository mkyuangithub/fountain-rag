package com.mkyuan.fountainbase.vectordb.bean.addpoint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PointsWrapper implements Serializable {
	private List<Point> points = new ArrayList<Point>();

	public List<Point> getPoints() {
		return points;
	}

	public void setPoints(List<Point> points) {
		this.points = points;
	}
}
