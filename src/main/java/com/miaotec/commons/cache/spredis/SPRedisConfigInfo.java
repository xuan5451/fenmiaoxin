/**
 * 
 */
package com.miaotec.commons.cache.spredis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.miaotec.commons.util.CommonConfigFactory;

/**
 * @author zhangyonghui
 *
 */
public class SPRedisConfigInfo {
	
	private static final Logger LOGGER = LogManager.getLogger(SPRedisConfigInfo.class);
	
	private SPRedisConfigInfo(){};
	private static SPRedisConfigInfo instance;
	public static SPRedisConfigInfo instance() {
		if(instance==null){
			instance=new SPRedisConfigInfo();
			instance.init();
		}			
		return instance;
	}

	//最大活动对象数
	private int maxActive;
	//最大空闲对象数
	private int maxIdle;
	//最长等待时间
	private int maxWait;
	private boolean testOnBorrow;
	private boolean testOnReturn;
	private String ip;
	private int port;
	private String password;
	
	private void init(){
		try {
			
			maxActive=Integer.valueOf(CommonConfigFactory.getConfigValue("redis.pool.maxActive"));
			maxIdle=Integer.valueOf(CommonConfigFactory.getConfigValue("redis.pool.maxIdle"));
			maxWait=Integer.valueOf(CommonConfigFactory.getConfigValue("redis.pool.maxWait"));
			testOnBorrow= Boolean.valueOf(CommonConfigFactory.getConfigValue("redis.pool.testOnBorrow"));
			testOnReturn= Boolean.valueOf(CommonConfigFactory.getConfigValue("redis.pool.testOnReturn"));
			
			ip=CommonConfigFactory.getConfigValue("redis.ip");
			String redisPort = CommonConfigFactory.getConfigValue("redis.port");
			if(redisPort!=null && redisPort.length()>0){
				port=Integer.valueOf(CommonConfigFactory.getConfigValue("redis.port"));
			}
			password=CommonConfigFactory.getConfigValue("redis.password");		
		} catch (Exception ex) {
			LOGGER.error("SPRedisConfigInfo-init:", ex);
		}
		
	}
	
	public int getMaxActive() {
		return maxActive;
	}
	
	public int getMaxIdle() {
		return maxIdle;
	}
	
	public int getMaxWait() {
		return maxWait;
	}
	
	public boolean getTestOnBorrow() {
		return testOnBorrow;
	}
	
	public boolean getTestOnReturn() {
		return testOnReturn;
	}	

	public String getIp() {
		return ip;
	}	

	public int getPort() {
		return port;
	}	

	public String getPassword() {
		return password;
	}
}
