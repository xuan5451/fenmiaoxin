/**
 * 
 */
package com.miaotec.commons.cache.redis;

import java.util.List;

import org.apache.commons.pool.impl.GenericObjectPool.Config;

import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

/**
 * @author zhangyonghui
 *
 */
public class RedisClientPool {

	private final ShardedJedisPool slavePool;
	private final ShardedJedisPool masterPool;
	
	public RedisClientPool(Config config,List<JedisShardInfo> masterClientList,List<JedisShardInfo> slaveClientList) {
		 this.masterPool = new ShardedJedisPool(config, masterClientList);
		 this.slavePool = new ShardedJedisPool(config,slaveClientList);
	}
	
	 public ShardedJedis getResource() {
	        try {
	            return (ShardedJedis) masterPool.getResource();
	        } catch (Exception e) {
	            throw new JedisConnectionException("获取jedis连接失败", e);
	        }
	 }
	 
	 public ShardedJedis getSlaveResource() {
	        try {
	            return (ShardedJedis) slavePool.getResource();
	        } catch (Exception e) {
	            throw new JedisConnectionException("获取jedis连接失败", e);
	        }
	 }
	        
	public void returnResource(final Object resource) {
	        try {
	        	masterPool.returnResource((ShardedJedis) resource);
	        } catch (Exception e) {
	            throw new JedisException("回收jedis连接失败", e);
	        }
	    }
	
	public void returnSlaveResource(final Object resource) {
        try {
        	slavePool.returnResource((ShardedJedis) resource);
        } catch (Exception e) {
            throw new JedisException("回收jedis连接失败", e);
        }
    }
	
	public void destoryMasterAll(){
		if (masterPool != null){
			masterPool.destroy();
		}	
	}
	public void destorySlaveAll(){
		if (slavePool != null){
			slavePool.destroy();
		}	
	}
}
