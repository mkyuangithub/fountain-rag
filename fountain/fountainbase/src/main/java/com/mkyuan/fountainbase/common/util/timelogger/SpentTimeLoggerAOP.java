/**
 * 系统项目名称 package com.mkyuan.aset.mall.util.aoplog SpentTimeLoggerAOP.java
 * 
 * Apr 27, 2021-2:17:47 PM 2021XX公司-版权所有
 * 
 */
package com.mkyuan.fountainbase.common.util.timelogger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 
 * SpentTimeLoggerAOP
 * 
 * 
 * Apr 27, 2021 2:17:47 PM
 * 
 * @version 1.0.0
 * 
 */
@Aspect
@Order(-1)
@Component
public class SpentTimeLoggerAOP {
	protected Logger logger = LogManager.getLogger(this.getClass());

	@Pointcut("@annotation(com.mkyuan.fountainbase.common.util.timelogger.TimeLogger)")
	public void timeLogger() {
	}

	/**
	 * 环绕增强，相当于MethodInterceptor
	 */
	@Around("timeLogger()")
	public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
		Object res = null;
		long time = System.currentTimeMillis();
		try {
			res = joinPoint.proceed();
			time = System.currentTimeMillis() - time;
			logger.info(">>>>>>方法 {}.{} ->运行总计耗费了: {} millseconds ",
					joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName(), time);
			return res;
		}catch (IllegalStateException e) {
			throw new IllegalStateException(e.getMessage(),e);
		} catch (Exception e) {
			logger.info(">>>>>>system error happened: " + e.getMessage(), e);
			throw new Exception(">>>>>>system error happened: " + e.getMessage(), e);
		}
	}
}
