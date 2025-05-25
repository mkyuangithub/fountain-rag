package com.mkyuan.fountainbase.ai;

public class CompleteResponse {

	private String content;

	private Integer durationMilliseconds;
	private Integer completionTokens;
	private Integer promptTokens;
	private Integer totalTokens;

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Integer getCompletionTokens() {
		return completionTokens;
	}

	public void setCompletionTokens(Integer completionTokens) {
		this.completionTokens = completionTokens;
	}

	public Integer getPromptTokens() {
		return promptTokens;
	}

	public void setPromptTokens(Integer promptTokens) {
		this.promptTokens = promptTokens;
	}

	public Integer getTotalTokens() {
		return totalTokens;
	}

	public void setTotalTokens(Integer totalTokens) {
		this.totalTokens = totalTokens;
	}

	public Integer getDurationMilliseconds() {
		return durationMilliseconds;
	}

	public void setDurationMilliseconds(Integer durationMilliseconds) {
		this.durationMilliseconds = durationMilliseconds;
	}
	
}