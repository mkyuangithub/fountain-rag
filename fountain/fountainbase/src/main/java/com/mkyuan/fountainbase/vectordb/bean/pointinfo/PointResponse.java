package com.mkyuan.fountainbase.vectordb.bean.pointinfo;

import java.io.Serializable;

public class PointResponse implements Serializable {

	private Result result = new Result();

	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}
}
