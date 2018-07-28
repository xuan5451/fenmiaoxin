/**
 * 
 */
package com.miaotec.commons.cache.spredis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.miaotec.commons.cache.BaseCacheKey;
import com.miaotec.commons.cache.ICacheKey;
import com.miaotec.commons.cache.ICacheService;

/**
 * @author zhangyonghui
 *
 */
public class SPRedisManager implements ICacheService {

	private SPRedisManager(){};
	private static JedisPool jedisPool;
	private static final SPRedisConfigInfo CONF_INFO=SPRedisConfigInfo.instance();
	private static final Logger LOGGER = LogManager.getLogger(SPRedisManager.class);
	private static SPRedisManager instance;
	public static SPRedisManager getInstance() {
		if(instance==null){
			instance=new SPRedisManager();
			init();
		}
		return instance;
	}
	
	private static void init(){
		try {
			JedisPoolConfig poolConfig=new JedisPoolConfig();
			poolConfig.setMaxActive(CONF_INFO.getMaxActive());
			poolConfig.setMaxIdle(CONF_INFO.getMaxIdle());
			poolConfig.setTestOnBorrow(CONF_INFO.getTestOnBorrow());
			poolConfig.setTestOnReturn(CONF_INFO.getTestOnReturn());
			
			jedisPool=new JedisPool(poolConfig, CONF_INFO.getIp(), CONF_INFO.getPort(), CONF_INFO.getMaxWait(),CONF_INFO.getPassword());
		} catch (Exception ex) {
			LOGGER.error("SPRedisManager-init error:", ex);
		}		
	}
	
	public void returnResource(JedisPool jedisPool,Jedis jedis) {
		if(jedis!=null)
			jedisPool.returnResource(jedis);
	}
	
	public String get(ICacheKey cacheKey) {
		String result=StringUtils.EMPTY;
		Jedis jedis=null;
		try {
			jedis= jedisPool.getResource();
			result= jedis.get(cacheKey.getKey());
		} catch (Exception ex) {
			jedisPool.returnBrokenResource(jedis);
		}finally{
			returnResource(jedisPool, jedis);
		}		
		return result;
	}
	
	public Object getObj(ICacheKey cacheKey) {
		Object obj=null;
		Jedis jedis=null;
		try {
			jedis= jedisPool.getResource();
			String result= jedis.get(cacheKey.getKey());
			if(StringUtils.isNotBlank(result)){
				obj=JSON.parseObject(result, new TypeReference<Object>(){});
			}
		} catch (Exception ex) {
			jedisPool.returnBrokenResource(jedis);
		}finally{
			returnResource(jedisPool, jedis);
		}		
		return obj;
	}
	
	public<T> T getMap(ICacheKey cacheKey,Class<T> clazz) {
		T obj = null;
		Jedis jedis=null;
		try {
			jedis= jedisPool.getResource();
			String result= jedis.get(cacheKey.getKey());
			if(StringUtils.isNotBlank(result)){
				obj=JSON.parseObject(result, clazz);
			}
		} catch (Exception ex) {
			jedisPool.returnBrokenResource(jedis);
		}finally{
			returnResource(jedisPool, jedis);
		}		
		return obj;
	}
	
	public void put(ICacheKey key, Object object) {    	
		Jedis jedis=null;
		try {
			jedis= jedisPool.getResource();
			String value=StringUtils.EMPTY;
			if(object.getClass().equals(String.class)){
				value=String.valueOf(object);
			}else{
				value=JSONObject.toJSONString(object);
			}			 
			jedis.set(key.getKey(), value);
			if(key.getExpirationTime()!=-1){
				jedis.expire(key.getKey(), key.getExpirationTime());
			}else if(key.getExpirationAtTime()>0){
				jedis.expireAt(key.getKey(), key.getExpirationAtTime());
			}
		} catch (Exception ex) {
			jedisPool.returnBrokenResource(jedis);
		}finally{
			returnResource(jedisPool, jedis);
		}
	}
	
	/**
	 * 自增操作 支持64bit integer
	 * @param cacheKey
	 * @return 返回自增后的值
	 */
	public long increase(ICacheKey cacheKey) {
		long result=0L;
		Jedis jedis=null;
		try {
			
			jedis= jedisPool.getResource();
			result=jedis.incr(cacheKey.getKey());
			if(cacheKey.getExpirationTime()>-1){
				jedis.expire(cacheKey.getKey(), cacheKey.getExpirationTime());
			}else if(cacheKey.getExpirationAtTime()>0){
				jedis.expireAt(cacheKey.getKey(), cacheKey.getExpirationAtTime());
			}
			
		} catch (Exception ex) {
			jedisPool.returnBrokenResource(jedis);
		}finally{
			returnResource(jedisPool, jedis);
		}
		return result;
	}
	
	public long decrBy(ICacheKey cacheKey, long value) {
		long result=0L;
		Jedis jedis=null;
		try {
			
			jedis= jedisPool.getResource();
			result=jedis.decrBy(cacheKey.getKey(), value);
			if(cacheKey.getExpirationTime()>-1){
				jedis.expire(cacheKey.getKey(), cacheKey.getExpirationTime());
			}else if(cacheKey.getExpirationAtTime()>0){
				jedis.expireAt(cacheKey.getKey(), cacheKey.getExpirationAtTime());
			}
			
		} catch (Exception ex) {
			jedisPool.returnBrokenResource(jedis);
		}finally{
			returnResource(jedisPool, jedis);
		}
		return result;
	}

	@Override
	public long decr(ICacheKey key) {
		long result=0L;
		Jedis jedis=null;
		try {
			jedis= jedisPool.getResource();			
			result=jedis.decr(key.getKey());
			if(key.getExpirationTime()>-1){
				jedis.expire(key.getKey(), key.getExpirationTime());
			}else if(key.getExpirationAtTime()>0){
				jedis.expireAt(key.getKey(), key.getExpirationAtTime());
			}
		} catch (Exception ex) {
			jedisPool.returnBrokenResource(jedis);
		}finally{
			returnResource(jedisPool, jedis);
		}
		return result;
	}

	@Override
	public void hashPut(ICacheKey hashKeyName, String key, Object value) {
		Jedis jedis=null;
		try {
			jedis= jedisPool.getResource();
			String val=StringUtils.EMPTY;
			if(value.getClass().equals(String.class)){
				val=String.valueOf(value);
			}else{
				val=JSONObject.toJSONString(value);
			}			 
			jedis.hset(hashKeyName.getKey(), key, val);
			if(hashKeyName.getExpirationTime()!=-1){
				jedis.expire(hashKeyName.getKey(), hashKeyName.getExpirationTime());
			}else if(hashKeyName.getExpirationAtTime()>0){
				jedis.expireAt(hashKeyName.getKey(), hashKeyName.getExpirationAtTime());
			}
		} catch (Exception ex) {
			jedisPool.returnBrokenResource(jedis);
		}finally{
			returnResource(jedisPool, jedis);
		}
		
	}

	@Override
	public long hashLength(ICacheKey hashKeyName) {
		long result=0L;
		Jedis jedis=null;
		try {
			jedis= jedisPool.getResource();			
			result=jedis.hlen(hashKeyName.getKey());
		} catch (Exception ex) {
			jedisPool.returnBrokenResource(jedis);
		}finally{
			returnResource(jedisPool, jedis);
		}
		return result;
	}

	@Override
	public String hashGet(ICacheKey hashKeyName, String key) {
		String result=StringUtils.EMPTY;
		Jedis jedis=null;
		try {
			jedis= jedisPool.getResource();			
			result=jedis.hget(hashKeyName.getKey(),key);
		} catch (Exception ex) {
			jedisPool.returnBrokenResource(jedis);
		}finally{
			returnResource(jedisPool, jedis);
		}
		return result;
	}

	@Override
	public <T> T hashGet(ICacheKey hashKeyName, String key, Class<T> clazz) {
		T obj=null;
		String result=hashGet(hashKeyName, key);
		
		return obj;
	}

	@Override
	public Set<String> hkeys(ICacheKey key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> hashVals(ICacheKey key) {
		List<String> list = new ArrayList<String>();
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			list = jedis.hvals(key.getKey());
		} catch (Exception ex) {
			jedisPool.returnBrokenResource(jedis);
		} finally {
			returnResource(jedisPool, jedis);
		}
		return list;
	}

	@Override
	public void hashMSet(ICacheKey key, Map<String, String> values) {
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			jedis.hmset(key.getKey(), values);
			if (key.getExpirationTime() != -1) {
				jedis.expire(key.getKey(), key.getExpirationTime());
			}else if(key.getExpirationAtTime()>0){
				jedis.expireAt(key.getKey(), key.getExpirationAtTime());
			}
		} catch (Exception ex) {
			jedisPool.returnBrokenResource(jedis);
		} finally {
			returnResource(jedisPool, jedis);
		}
	}

	@Override
	public void del(ICacheKey key) {
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			jedis.del(key.getKey());
		} catch (Exception ex) {
			jedisPool.returnBrokenResource(jedis);
		} finally {
			returnResource(jedisPool, jedis);
		}
	}

	@Override
	public void hashDel(ICacheKey hashKeyName, String key) {
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			jedis.hdel(hashKeyName.getKey(), key);
		} catch (Exception ex) {
			jedisPool.returnBrokenResource(jedis);
		} finally {
			returnResource(jedisPool, jedis);
		}
	}

	@Override
	public boolean hashExists(ICacheKey hashKeyName, String key) {
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			return jedis.hexists(hashKeyName.getKey(), key);
		} catch (Exception ex) {
			jedisPool.returnBrokenResource(jedis);
		} finally {
			returnResource(jedisPool, jedis);
		}
		return false;
	}


	@Override
	public long hashIncrBy(ICacheKey key, String field, long value) {
		long result = 0;
		Jedis jedis = null;
		try {

			jedis = jedisPool.getResource();
			result = jedis.hincrBy(key.getKey(), field, value);
			if (key.getExpirationTime() > -1) {
				jedis.expire(key.getKey(), key.getExpirationTime());
			} else if (key.getExpirationAtTime() > 0) {
				jedis.expireAt(key.getKey(), key.getExpirationAtTime());
			}

		} catch (Exception ex) {
			jedisPool.returnBrokenResource(jedis);
		} finally {
			returnResource(jedisPool, jedis);
		}
		return result;
	}

	@Override
	public long ttl(ICacheKey key) {
		long result=0;
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			result=jedis.ttl(key.getKey());
		} catch (Exception ex) {
			jedisPool.returnBrokenResource(jedis);
		} finally {
			returnResource(jedisPool, jedis);
		}
		return result;
	}

	@Override
	public void exprie(ICacheKey key) {
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			jedis.expire(key.getKey(),key.getExpirationTime());
		} catch (Exception ex) {
			jedisPool.returnBrokenResource(jedis);
		} finally {
			returnResource(jedisPool, jedis);
		}		
	}

	@Override
	public void push(ICacheKey key, String value) {
		Jedis jedis = null;
		try {

			jedis = jedisPool.getResource();
			jedis.rpush(key.getKey(), value);
			if (key.getExpirationTime() > -1) {
				jedis.expire(key.getKey(), key.getExpirationTime());
			} else if (key.getExpirationAtTime() > 0) {
				jedis.expireAt(key.getKey(), key.getExpirationAtTime());
			}

		} catch (Exception ex) {
			jedisPool.returnBrokenResource(jedis);
		} finally {
			returnResource(jedisPool, jedis);
		}
		
	}

	@Override
	public String pop(ICacheKey key) {
		Jedis jedis = null;
		try {

			jedis = jedisPool.getResource();
			return jedis.lpop(key.getKey());

		} catch (Exception ex) {
			jedisPool.returnBrokenResource(jedis);
		} finally {
			returnResource(jedisPool, jedis);
		}
		return null;
	}

	@Override
	public List<String> hashMGet(ICacheKey key, String... fields) {
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			return jedis.hmget(key.getKey(), fields);
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			returnResource(jedisPool, jedis);
		}
		return null;
	}

	@Override
	public long decrBy(BaseCacheKey key, Long amount) {
		long result=0L;
		Jedis jedis=null;
		try {
			
			jedis= jedisPool.getResource();
			result=jedis.decrBy(key.getKey(),amount);
			if(key.getExpirationTime()>-1){
				jedis.expire(key.getKey(), key.getExpirationTime());
			}else if(key.getExpirationAtTime()>0){
				jedis.expireAt(key.getKey(), key.getExpirationAtTime());
			}
			
		} catch (Exception ex) {
			jedisPool.returnBrokenResource(jedis);
		}finally{
			returnResource(jedisPool, jedis);
		}
		return result;
	}
}
