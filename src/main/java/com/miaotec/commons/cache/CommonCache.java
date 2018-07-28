/**
 * 
 */
package com.miaotec.commons.cache;

import com.miaotec.commons.cache.redis.RedisManager;
import com.miaotec.commons.cache.spredis.SPRedisManager;
import com.miaotec.commons.util.CommonConfigFactory;



/**
 * @author zhangyonghui
 *
 *缓存工厂类
 */
public class CommonCache {

	private CommonCache(){};
	private static ICacheService instance;
	static{
		String cacheType=CommonConfigFactory.getConfigValue("cache.type");
		if(cacheType.equalsIgnoreCase("single")){
			instance=SPRedisManager.getInstance();
		}else if(cacheType.equalsIgnoreCase("distributed")){
			instance=RedisManager.getInstance();
		}		
	}
	public static ICacheService getInstance() {
		return instance;
	}
}
