
package com.mkyuan.fountainbase.autoconfig.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * 
 * RedissonProperties
 * 
 * 
 * Feb 20, 2021 1:56:09 PM
 * 
 * @version 1.0.0
 * 
 */
//@Configuration
@Component
public class RedissonProperties {

	@Value("${spring.redis.timeout}")
	private int timeout;
	@Value("${spring.redis.password}")
	private String password;
	@Value("${spring.redis.database:0}")
	private int database = 0;
	@Value("${spring.redis.jedis.pool.max-active:50}")
	private int connectionPoolSize = 50;
	@Value("${spring.redis.jedis.pool.min-idle:1}")
	private int connectionMinimumIdleSize = 1;
	@Value("${spring.redis.jedis.pool.max-active:50}")
	private int slaveConnectionPoolSize = 50;
	@Value("${spring.redis.jedis.pool.max-active:50}")
	private int masterConnectionPoolSize = 50;
	@Value("${spring.redis.redisson.nodes}")
	private String[] sentinelAddresses;
	@Value("${spring.redis.sentinel.master}")
	private String masterName;

	@Value("${spring.redis.sentinel.subscriptionsPerConnection}")
	int subscriptionsPerConnection = 50;

	@Value("${spring.redis.sentinel.subscriptionConnectionPoolSize}")
	int subscriptionConnectionPoolSize = 200;

	public int getSubscriptionsPerConnection() {
		return subscriptionsPerConnection;
	}

	public void setSubscriptionsPerConnection(int subscriptionsPerConnection) {
		this.subscriptionsPerConnection = subscriptionsPerConnection;
	}

	public int getSubscriptionConnectionPoolSize() {
		return subscriptionConnectionPoolSize;
	}

	public void setSubscriptionConnectionPoolSize(int subscriptionConnectionPoolSize) {
		this.subscriptionConnectionPoolSize = subscriptionConnectionPoolSize;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getSlaveConnectionPoolSize() {
		return slaveConnectionPoolSize;
	}

	public void setSlaveConnectionPoolSize(int slaveConnectionPoolSize) {
		this.slaveConnectionPoolSize = slaveConnectionPoolSize;
	}

	public int getMasterConnectionPoolSize() {
		return masterConnectionPoolSize;
	}

	public void setMasterConnectionPoolSize(int masterConnectionPoolSize) {
		this.masterConnectionPoolSize = masterConnectionPoolSize;
	}

	public String[] getSentinelAddresses() {
		return sentinelAddresses;
	}

	public void setSentinelAddresses(String sentinelAddresses) {
		this.sentinelAddresses = sentinelAddresses.split(",");
	}

	public String getMasterName() {
		return masterName;
	}

	public void setMasterName(String masterName) {
		this.masterName = masterName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getConnectionPoolSize() {
		return connectionPoolSize;
	}

	public void setConnectionPoolSize(int connectionPoolSize) {
		this.connectionPoolSize = connectionPoolSize;
	}

	public int getConnectionMinimumIdleSize() {
		return connectionMinimumIdleSize;
	}

	public void setConnectionMinimumIdleSize(int connectionMinimumIdleSize) {
		this.connectionMinimumIdleSize = connectionMinimumIdleSize;
	}

	public int getDatabase() {
		return database;
	}

	public void setDatabase(int database) {
		this.database = database;
	}

	public void setSentinelAddresses(String[] sentinelAddresses) {
		this.sentinelAddresses = sentinelAddresses;
	}
}
