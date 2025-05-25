package com.mkyuan.fountainbase.autoconfig.asynctask;

/**
 * 异步线程池配置项
 * 
 * @author Administrator
 *
 */
public class AsyncTaskExecutorProperties {

	private Integer corePoolSize;
	private Integer maxPoolSize;
	private Integer queueCapacity;
	private Integer keepAliveSeconds;
	private Integer awaitTerminationSeconds;
	private Boolean waitForTasksToCompleteOnShutdown;
	private String threadNamePrefix;

	public Integer getCorePoolSize() {
		return corePoolSize;
	}

	public void setCorePoolSize(Integer corePoolSize) {
		this.corePoolSize = corePoolSize;
	}

	public Integer getMaxPoolSize() {
		return maxPoolSize;
	}

	public void setMaxPoolSize(Integer maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}

	public Integer getQueueCapacity() {
		return queueCapacity;
	}

	public void setQueueCapacity(Integer queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

	public Integer getKeepAliveSeconds() {
		return keepAliveSeconds;
	}

	public void setKeepAliveSeconds(Integer keepAliveSeconds) {
		this.keepAliveSeconds = keepAliveSeconds;
	}

	public Integer getAwaitTerminationSeconds() {
		return awaitTerminationSeconds;
	}

	public void setAwaitTerminationSeconds(Integer awaitTerminationSeconds) {
		this.awaitTerminationSeconds = awaitTerminationSeconds;
	}

	public String getThreadNamePrefix() {
		return threadNamePrefix;
	}

	public void setThreadNamePrefix(String threadNamePrefix) {
		this.threadNamePrefix = threadNamePrefix;
	}

	public Boolean getWaitForTasksToCompleteOnShutdown() {
		return waitForTasksToCompleteOnShutdown;
	}

	public void setWaitForTasksToCompleteOnShutdown(Boolean waitForTasksToCompleteOnShutdown) {
		this.waitForTasksToCompleteOnShutdown = waitForTasksToCompleteOnShutdown;
	}

}
