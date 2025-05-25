package com.mkyuan.fountainbase.knowledge.bean;

import java.io.Serializable;

public class VectorIndexBean implements Serializable {
	private String field_name;
	private FieldSchema field_schema;

	public String getField_name() {
		return field_name;
	}

	public void setField_name(String field_name) {
		this.field_name = field_name;
	}

	public FieldSchema getField_schema() {
		return field_schema;
	}

	public void setField_schema(FieldSchema field_schema) {
		this.field_schema = field_schema;
	}
}
