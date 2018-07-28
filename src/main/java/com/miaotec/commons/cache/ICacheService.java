/**
 * 
 */
package com.miaotec.commons.cache;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author zhangyonghui
 * 
 * 公共缓存服务，根据配置会有不同的缓存实现
 *
 */
public interface ICacheService {

	/**
	 * 根据缓存key获取string 类型 value
	 * @param cacheKey
	 * @return
	 */
	public String get(ICacheKey cacheKey);
	
	/**
	 * 根据缓存key获取object类型 value
	 * @param cacheKey
	 * @return
	 */
	public Object getObj(ICacheKey cacheKey);
	
	/**
	 * 根据缓存key获取泛型 value
	 * @param cacheKey
	 * @param clazz
	 * @return
	 */
	public<T> T getMap(ICacheKey cacheKey,Class<T> clazz);
	
	/**
	 * 插入缓存值
	 * @param key
	 * @param object
	 */
	public void put(ICacheKey key, Object object);	
	
	/**
	 * 自增操作
	 * @param cacheKey
	 * @return
	 */
	public long increase(ICacheKey cacheKey);
	
	/**
	 * 递减操作
	 * @param key
	 * @return
	 */
	public long decr(ICacheKey key);
	/**
	 * 根据value自减
	 * @param key
	 * @param value
	 * @return
	 */
	public long decrBy(ICacheKey key, long value);
	
	
	/******************针对Hash 的操作**********************/
	
	/**
	 * 插入Hash表值
	 * @param hashKeyName
	 * @param key
	 * @param value
	 */
	public void hashPut(ICacheKey hashKeyName,String key,Object value);
	
	/**
	 * 获取Hash表大小
	 * @param hashKeyName
	 * @return
	 */
	public long hashLength(ICacheKey hashKeyName);
	
	/**
	 * 获取Hash值 String格式
	 * @param hashKeyName
	 * @param key
	 * @return
	 */
	public String hashGet(ICacheKey hashKeyName,String key);
	
	/**
	 * 获取Hash值 泛型方法
	 * @param hashKeyName
	 * @param key
	 * @param clazz
	 * @return
	 */
	public<T> T hashGet(ICacheKey hashKeyName,String key,Class<T> clazz);
	
	/**
	 * 获取Hash表中所有Key值集合
	 * @param key
	 * @return
	 */
	public Set<String> hkeys(ICacheKey key);
	
	public List<String> hashVals(ICacheKey key);
	
	public void hashMSet(ICacheKey key, Map<String, String> values);
	
	public List<String> hashMGet(ICacheKey key, String ...fields);
	
	public void hashDel(ICacheKey hashKeyName, String key);
	
	public boolean hashExists(ICacheKey hashKeyName, String key);
	
	/******************针对Hash 的操作**********************/
	
	public void del(ICacheKey key);
	
	/**
	 * 获取缓存剩余时间  秒数
	 * @param key
	 * @return
	 */
	public long ttl(ICacheKey key);
	
	/**
	 * 设置过期时间 expireTime  相对过期时间
	 * @param key
	 */
	public void exprie(ICacheKey key);

	long hashIncrBy(ICacheKey key, String field, long value);
	/**
	 * 往队列里从右边塞入
	 * @param listKey
	 * @param value
	 */
	void push(ICacheKey listKey,String value);
	/**
	 * 从队列最左边取，取不到返回null
	 * @param listKey
	 * @return
	 */
	String pop(ICacheKey listKey);

	public long decrBy(BaseCacheKey key, Long amount);

}
