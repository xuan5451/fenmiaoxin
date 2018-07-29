package com.miaotec.commons.cache;

import org.apache.commons.lang3.StringUtils;

public class BaseCacheKey implements ICacheKey {
	private String key;
	/**
	 * 子key 业务唯一标示 如 用户id 支付流水号等111
	 */
	private String subKey;

	/**
	 * 缓存过期时间 单位:秒 默认永不过期
	 */
	private int expireTime = -1;

	/**
	 * 分隔符
	 */
	private String splitStr = "_";

	public BaseCacheKey(String key, String subKey) {
		super();
		this.key = key;
		this.subKey = subKey;
	}

	public BaseCacheKey(String key) {
		super();
		this.key = key;
	}

	@Override
	public String getKey() {
		StringBuilder sBuilder = new StringBuilder();
		if (StringUtils.isBlank(key)) {
			throw new RuntimeException("未设置缓存key");
		}
		sBuilder.append(key);
		if (StringUtils.isNotBlank(subKey)) {
			sBuilder.append(splitStr);
			sBuilder.append(subKey);
		}
		return sBuilder.toString();
	}
	/**
	 * 设置过期时间
	 */
	@Override
	public int getExpirationTime() {
		return expireTime;
	}

	@Override
	public long getExpirationAtTime() {

		return 0;
	}

	@Override
	public String getValueFromSource() {
		return CommonCache.getInstance().get(this);
	}

	@Override
	public int getLocalCacheTime() {

		return 0;
	}

	public int getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(int expireTime) {
		this.expireTime = expireTime;
	}
	
}
