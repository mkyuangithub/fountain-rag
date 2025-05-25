package com.mkyuan.fountainbase.knowledge.bean;

import java.io.Serializable;

public class FieldSchema implements Serializable {
	private String type;
	private String tokenizer;
	private int min_token_len;
	private int max_token_len;
	private boolean lowercase;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTokenizer() {
		return tokenizer;
	}

	public void setTokenizer(String tokenizer) {
		this.tokenizer = tokenizer;
	}

	public int getMin_token_len() {
		return min_token_len;
	}

	public void setMin_token_len(int min_token_len) {
		this.min_token_len = min_token_len;
	}

	public int getMax_token_len() {
		return max_token_len;
	}

	public void setMax_token_len(int max_token_len) {
		this.max_token_len = max_token_len;
	}

	public boolean isLowercase() {
		return lowercase;
	}

	public void setLowercase(boolean lowercase) {
		this.lowercase = lowercase;
	}
}
