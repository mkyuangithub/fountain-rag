package com.mkyuan.fountainbase.autoconfig.asynctask;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.scheduling.annotation.Async;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Async
public @interface AsyncTask {

	@AliasFor(value = "value", annotation = Async.class)
	String value() default AsyncConfig.DATA_FEED_NAME;

}