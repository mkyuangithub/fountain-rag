/**
 * 系统项目名称 package com.mkyuan.aset.mall.util.aoplog TimeLogger.java
 * 
 * Apr 27, 2021-2:14:18 PM 2021XX公司-版权所有
 * 
 */
package com.mkyuan.fountainbase.common.util.timelogger;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import java.lang.annotation.Target;

/**
 * 
 * TimeLogger
 * 
 * 
 * Apr 27, 2021 2:14:18 PM
 * 
 * @version 1.0.0
 * 
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TimeLogger {

}
