package com.mkyuan.fountainbase.autoconfig.asynctask;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

	public static final String DATA_FEED_NAME = "dataFeedExecutor";

	// 默认异步线程池
	@Override
	public Executor getAsyncExecutor() {
		ThreadPoolTaskExecutor indexExecutor = new ThreadPoolTaskExecutor();
		indexExecutor.setCorePoolSize(50);
		indexExecutor.setMaxPoolSize(50);
		indexExecutor.setQueueCapacity(1000);
		indexExecutor.setThreadNamePrefix("AsyncExecutor-");
		indexExecutor.initialize();
		return indexExecutor;
	}

	// 知识库数据导入线程池
	@Bean(name = DATA_FEED_NAME)
	public AsyncTaskExecutor asyncTaskExecutor() {
		AsyncTaskExecutorProperties properties = new AsyncTaskExecutorProperties();
		properties.setCorePoolSize(4);
		properties.setMaxPoolSize(8);
		properties.setQueueCapacity(512);
		properties.setKeepAliveSeconds(200);
		properties.setAwaitTerminationSeconds(120);
		properties.setWaitForTasksToCompleteOnShutdown(true);
		properties.setThreadNamePrefix("DataFeedExecutor-");
		AsyncTaskExecutor executor = new AsyncTaskExecutor(properties);
		executor.initialize();
		return executor;
	}

}
