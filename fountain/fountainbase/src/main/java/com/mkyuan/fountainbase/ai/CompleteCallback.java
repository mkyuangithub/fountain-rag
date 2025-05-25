package com.mkyuan.fountainbase.ai;

public interface CompleteCallback {
	
	void onData(CompleteResponseChunk chunk);
	
	void onDone();
	
	void onError(Exception e);
	
}