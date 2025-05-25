package com.mkyuan.fountainbase.autoconfig.asynctask;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 自定义异步线程池
 * 
 * @author Administrator
 *
 */
public class AsyncTaskExecutor extends ThreadPoolTaskExecutor {

	private static final long serialVersionUID = 1L;

	public AsyncTaskExecutor(AsyncTaskExecutorProperties properties) {
		this.setCorePoolSize(properties.getCorePoolSize());
		this.setMaxPoolSize(properties.getMaxPoolSize());
		this.setQueueCapacity(properties.getQueueCapacity());
		this.setKeepAliveSeconds(properties.getKeepAliveSeconds());
		this.setThreadNamePrefix(properties.getThreadNamePrefix());
		this.setAwaitTerminationSeconds(properties.getAwaitTerminationSeconds());
		this.setWaitForTasksToCompleteOnShutdown(properties.getWaitForTasksToCompleteOnShutdown());
		this.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
	}

}