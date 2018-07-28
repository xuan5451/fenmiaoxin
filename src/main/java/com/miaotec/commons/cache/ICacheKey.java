/**
 * 
 */
package com.miaotec.commons.cache;

/**
 * @author zhangyonghui
 *
 */
public interface ICacheKey {

	/**
	 * cache key对key统一管理
	 * @return Key
	 */
	String getKey();
	/**
	 * cache timeout
	 * 绝对过期时间（秒），-1 代表永不过期
	 * @return ExpirationTime
	 */
	int getExpirationTime();
	
	/**
	 * 获取缓存过期的时间点，赋值为UNIX时间戳
	 * @return
	 */
	long getExpirationAtTime();
	/**
	 * 重新从数据来源中获取数据
	 * @return Object
	 */
	Object getValueFromSource();
	
	/**
	 * 本地缓存时间
	 * @return
	 */
	int getLocalCacheTime();
}
