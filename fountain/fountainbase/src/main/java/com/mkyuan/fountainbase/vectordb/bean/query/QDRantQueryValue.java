package com.mkyuan.fountainbase.vectordb.bean.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class QDRantQueryValue implements Serializable {

	public String text="";
	public List<String> any = new ArrayList<>();

	public List<String> except = new ArrayList<>();

	private String value = "";

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public List<String> getAny() {
		return any;
	}

	public void setAny(List<String> any) {
		this.any = any;
	}

	public List<String> getExcept() {
		return except;
	}

	public void setExcept(List<String> except) {
		this.except = except;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
