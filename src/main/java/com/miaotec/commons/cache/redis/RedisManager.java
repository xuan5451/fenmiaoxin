/**
 * 
 */
package com.miaotec.commons.cache.redis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ShardedJedis;
import redis.clients.util.ShardInfo;

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
public class RedisManager implements ICacheService{  
	 
	
	RedisManagerProxy proxy;
	
	/**
	 * 连接ip格式，默认第一个为主服务器,后面的为从服务器,若只设置一个，则为主服务器
	 */
	public static final String DEFAULTIPFORMAT = "127.0.0.1:6379,127.0.0.1:6380"; 
	
	static String ips = null;
	
	/**
	 * 支持读写分离
	 */
	boolean enableReadWriteSeparation = true;
	
	ThreadLocal<OP> cop = new ThreadLocal<OP>();
	
	public static final Logger logger = LoggerFactory.getLogger(RedisManagerProxy.class);
	
	/**
	 * 安全创建单例对象
	 */
	private static class StaticHolder {
	  static final RedisManagerProxy proxy = new RedisManagerProxy(ips);
	  static final RedisManager instance = (RedisManager) proxy.getProxy(new RedisManager(proxy));
	}
	  
	public static RedisManager getInstance(String ips){
	   RedisManager.ips = ips;
	   return StaticHolder.instance;
	} 
	
	public static RedisManager getInstance(){
	   return StaticHolder.instance;   
	} 
  
	
    public RedisManager() {
		super();
	}


	private RedisManager(RedisManagerProxy proxy){
    	this.proxy = proxy;
    }
    
    private void initOP(OP op){
    	if(enableReadWriteSeparation){
    		cop.set(op);
    	}else{
    		cop.set(OP.READORWRITE);
    	}
    }
    
    public void enableReadWriteSeparation(boolean enable){
    	enableReadWriteSeparation = enable;
    }
    
    public void del(ICacheKey key) {
    	initOP(OP.WRITE);
    	proxy.getCurrJedis().del(key.getKey());
    }  
    
  //************************以下针对单字符串的操作***********************//  
  
    public long expire(ICacheKey key){
    	initOP(OP.WRITE);
    	ShardedJedis jedis = proxy.getCurrJedis();
    	if(key.getExpirationTime()>0){
    		return jedis.expire(key.getKey(), key.getExpirationTime());
		} else if (key.getExpirationAtTime() > 0) {
			jedis.expireAt(key.getKey(), key.getExpirationAtTime());
    	}
    	return -1;
    }
    
    public void put(ICacheKey key, String value) {
    	initOP(OP.WRITE);
    	ShardedJedis jedis = proxy.getCurrJedis();
    	jedis.set(key.getKey(), value);
    	if(key.getExpirationTime() != -1){
    		jedis.expire(key.getKey(), key.getExpirationTime());
		} else if (key.getExpirationAtTime() > 0) {
			jedis.expireAt(key.getKey(), key.getExpirationAtTime());
		}
    }
    
    public void put(ICacheKey key, byte[] object) {
    	initOP(OP.WRITE);
    	ShardedJedis jedis = proxy.getCurrJedis();
    	jedis.set(key.getKey().getBytes(), object);
    	if(key.getExpirationTime() != -1){
    		jedis.expire(key.getKey().getBytes(), key.getExpirationTime());
		} else if (key.getExpirationAtTime() > 0) {
			jedis.expireAt(key.getKey(), key.getExpirationAtTime());
		}
    }
    
//    public void flush(String... keys) {
//    	initOP(OP.WRITE);
//    	proxy.getCurrJedis().del(keys); 
//    }
//    
//    public void flushByte(String... keys) {
//    	initOP(OP.WRITE);
//    	byte[][] b = new byte[keys.length][];
//    	for (int i=0;i<b.length;i++){
//    		b[i] = keys[i].getBytes();
//    	}
//    	proxy.getCurrJedis().del(b); 
//    }
//    
    public String get(ICacheKey key) {
    	initOP(OP.READ);
        return  proxy.getCurrJedis().get(key.getKey());
    }    
    
    public byte[] getByte(ICacheKey key) {
    	initOP(OP.READ);
        return  proxy.getCurrJedis().get(key.getKey().getBytes());
    }
    
    public boolean exists(ICacheKey key){
    	initOP(OP.READ);
    	return proxy.getCurrJedis().exists(key.getKey());	
    } 
    
    public boolean existsByte(ICacheKey key){
    	initOP(OP.READ);
    	return proxy.getCurrJedis().exists(key.getKey().getBytes());	
    } 
    
    //*********************以下针对list的push,pop操作********************//
    /**
     * 建议用这个方法
     * @param listKeyName
     * @param object
     */
    
    public void push(ICacheKey listKeyName,String object){
    	initOP(OP.WRITE);
    	ShardedJedis jedis = proxy.getCurrJedis();
    	jedis.rpush(listKeyName.getKey(), object);
    	if(listKeyName.getExpirationTime() != -1){
    		jedis.expire(listKeyName.getKey(), listKeyName.getExpirationTime());
    	}else if(listKeyName.getExpirationAtTime()>0){
			jedis.expireAt(listKeyName.getKey(), listKeyName.getExpirationAtTime());
		}
    }
    public String getHost(ICacheKey key){
    	ShardedJedis jedis = proxy.getCurrJedis();
    	ShardInfo info = jedis.getShardInfo(key.getKey());
    	return info.toString()+":"+info.getName();
    }
    /**
     * 不建议用这个方法，因为getbytes没指定字符集的话，用的是系统默认，如果放和取的系统默认字符集不一样会有问题
     * @param listKeyName
     * @param objects
     */
    public void push(ICacheKey listKeyName,byte[] objects){
    	initOP(OP.WRITE);
    	ShardedJedis jedis = proxy.getCurrJedis();
    	jedis.rpush(listKeyName.getKey().getBytes(), objects);
    	if(listKeyName.getExpirationTime() != -1){
    		jedis.expire(listKeyName.getKey().getBytes(), listKeyName.getExpirationTime());
    	}else if(listKeyName.getExpirationAtTime()>0){
			jedis.expireAt(listKeyName.getKey(), listKeyName.getExpirationAtTime());
		}
    }
    
    public List<String> pull(ICacheKey listKeyName){
    	initOP(OP.READ);
    	ShardedJedis jedis = proxy.getCurrJedis();
    	return jedis.lrange(listKeyName.getKey(), 0, jedis.llen(listKeyName.getKey()));
    }

    public List<String> pull(ICacheKey listKeyName,int fromIndex,int lastIndex){
    	initOP(OP.READ);
    	ShardedJedis jedis = proxy.getCurrJedis();
    	return jedis.lrange(listKeyName.getKey(), fromIndex, lastIndex); 
    }
    
    public String pull(ICacheKey listKeyName,int index){
    	initOP(OP.READ);
    	ShardedJedis jedis = proxy.getCurrJedis();
    	return jedis.lindex(listKeyName.getKey(), index); 
    }
    
    public long length(ICacheKey listKeyName){
    	initOP(OP.READ);
    	ShardedJedis jedis = proxy.getCurrJedis();
    	return jedis.llen(listKeyName.getKey()); 
    }

	//private ConcurrentHashMap<String,BlockingPoPThread> popThreads = new ConcurrentHashMap<String,BlockingPoPThread>();
	/**
	 * 建议用这个方法
	 * @param listKeyName
	 * @return 取不到返回null
	 */
    public String pop(ICacheKey listKeyName){
    	initOP(OP.READ);
    	ShardedJedis jedis = proxy.getCurrJedis();
    	return jedis.lpop(listKeyName.getKey());
    }
    
    public String pop(ICacheKey listKeyName,boolean blockingWhenEmpty){
    	//一直阻塞状态
    	return null;//pop(listKeyName,blockingWhenEmpty);
    }

    public byte[] popByte(ICacheKey listKeyName){
    	initOP(OP.WRITE);
    	ShardedJedis jedis = proxy.getCurrJedis();
    	if(listKeyName.getExpirationTime() != -1){
    		jedis.expire(listKeyName.getKey().getBytes(), listKeyName.getExpirationTime());
		} else if (listKeyName.getExpirationAtTime() > 0) {
			jedis.expireAt(listKeyName.getKey(), listKeyName.getExpirationAtTime());
    	}
    	return jedis.lpop(listKeyName.getKey().getBytes());
    }
    
    public byte[] popByte(ICacheKey listKeyName,boolean blockingWhenEmpty){
    	initOP(OP.WRITE);
    	if (blockingWhenEmpty){
    		/**
    		 * 当为空，阻塞
    		 * 异步调用监控
    		 */
    		return null;
    	}else{
    		ShardedJedis jedis = proxy.getCurrJedis();
        	if(listKeyName.getExpirationTime() != -1){
        		jedis.expire(listKeyName.getKey().getBytes(), listKeyName.getExpirationTime());
    		} else if (listKeyName.getExpirationAtTime() > 0) {
    			jedis.expireAt(listKeyName.getKey(), listKeyName.getExpirationAtTime());
        	}
        	return jedis.lpop(listKeyName.getKey().getBytes());
    	}
    }
    
    
    //***********************以下针对hash表操作*********************//
    
    private void hashPutStr(ICacheKey hashKeyName,String key,String value){
    	initOP(OP.WRITE);
    	ShardedJedis jedis = proxy.getCurrJedis();
    	jedis.hset(hashKeyName.getKey(), key, value);
    	if(hashKeyName.getExpirationTime() != -1){
    		jedis.expire(hashKeyName.getKey(), hashKeyName.getExpirationTime());
		} else if (hashKeyName.getExpirationAtTime() > 0) {
			jedis.expireAt(hashKeyName.getKey(), hashKeyName.getExpirationAtTime());
		}
    }
    
    public long hashLength(ICacheKey hashKeyName){
    	initOP(OP.READ);
    	return proxy.getCurrJedis().hlen(hashKeyName.getKey());
    }
    
    public String hashGet(ICacheKey hashKeyName,String key){
    	initOP(OP.READ);
    	return proxy.getCurrJedis().hget(hashKeyName.getKey(), key);
    }
    
    public void hashPut(ICacheKey hashKeyName,String key,byte[] value){
    	initOP(OP.WRITE);
    	ShardedJedis jedis = proxy.getCurrJedis();
    	jedis.hset(hashKeyName.getKey().getBytes(), key.getBytes(), value);
    	if(hashKeyName.getExpirationTime() != -1){
    		jedis.expire(hashKeyName.getKey().getBytes(), hashKeyName.getExpirationTime());
    	}else if(hashKeyName.getExpirationAtTime()>0){
			jedis.expireAt(hashKeyName.getKey(), hashKeyName.getExpirationAtTime());
		}
    }
    
    public void hashByteLength(ICacheKey hashKeyName){
    	initOP(OP.READ);
    	proxy.getCurrJedis().hlen(hashKeyName.getKey().getBytes());
    }
   
    public byte[] hashByteGet(ICacheKey hashKeyName,String key){
    	initOP(OP.READ);
    	return proxy.getCurrJedis().hget(hashKeyName.getKey().getBytes(), key.getBytes());
    }
    
    

    /**
     * 以Object形式写入缓存
     * @param key
     * @param object
     */
    public void put(ICacheKey key, Object object) {
    	initOP(OP.WRITE);
    	ShardedJedis jedis = proxy.getCurrJedis();
    	String toJsonStr = org.apache.commons.lang3.StringUtils.EMPTY;
    	if(object.getClass().equals(String.class)){
    		toJsonStr=String.valueOf(object);
    	}else{
    		toJsonStr=JSON.toJSONString(object);	
    	}
    	jedis.set(key.getKey(), toJsonStr);
    	if(key.getExpirationTime() != -1){
    		jedis.expire(key.getKey(), key.getExpirationTime());
    	}else if(key.getExpirationAtTime()>0){
			jedis.expireAt(key.getKey(), key.getExpirationAtTime());
		}
    }
    
    /**
     * 获取字符串
     * @param key
     * @return
     */
    public String getStr(ICacheKey key) {
    	initOP(OP.READ);
    	return proxy.getCurrJedis().get(key.getKey());
    }
    
    /**
     * 获取Object
     * @param key
     * @return
     */
    public Object getObj(ICacheKey key) {
    	initOP(OP.READ);
    	String cacheValue = proxy.getCurrJedis().get(key.getKey());
    	return JSON.parseObject(cacheValue, new TypeReference<Object>(){});
    }
  
    /**
     * 通过指定类获取Map
     * @param key
     * @param clazz
     * @return
     */
    public <T> T getMap(ICacheKey key,Class<T> clazz) {
    	initOP(OP.READ);
    	String cacheValue = proxy.getCurrJedis().get(key.getKey());
    	T t = JSON.parseObject(cacheValue, clazz);
    	return t;
    }
    
    /**
     * 通过TypeReference获取Map
     * @param key
     * @param clazz
     * @return
     */
    public <K,V> Map<K,V> getMap(ICacheKey key, TypeReference<Map<K, V>> clazz){
    	initOP(OP.READ);
    	String cacheValue = proxy.getCurrJedis().get(key.getKey());
        Map<K,V> map =  (Map<K, V>) JSON.parseObject(cacheValue, clazz);
    	return map;
    }
    
    /**
     * 获取List
     * @param key
     * @param clazz
     * @return
     */
    public <T> List<T> getList(ICacheKey key,Class<T> clazz) {
    	initOP(OP.READ);
    	String cacheValue = proxy.getCurrJedis().get(key.getKey());
    	List<T> vList = JSON.parseArray(cacheValue,clazz);
    	return vList;
    }
    /**
     * 批量取缓存对象     
     * @param keyList 所有KEY的list集合
     * @param clazz 缓存对象类型
     * @return 返回一个缓存对象List集合
     */
    public <T> List<T> mgetObjList(List<ICacheKey> keyList,Class<T> clazz){
    	initOP(OP.READ);
    	List<T> resultList = null;
    	List<String> resultStringList = null;
    	List<String> clientInfoList = null;
    	if (keyList!=null&&keyList.size()>0) {
    		resultStringList = new ArrayList<String>();
    		resultList = new ArrayList<T>();
    		clientInfoList =  new ArrayList<String>();
    		String keys [] = new String[keyList.size()];
        	for (int i = 0; i < keyList.size(); i++) {
    			keys[i]= keyList.get(i).getKey();
    		}
        	for (int i = 0; i < keys.length; i++) {
        		//取出每一个KEY所在节点的Jedis实例 
        		Jedis jedis = proxy.getCurrJedis().getShard(keys[i]);
        		String clientInfo = jedis.getClient().getHost()+jedis.getClient().getPort();
        		//判断是否mget过，如果没有mget过则把信息加入clientInfoList并使用该实例mget所有元素
        		if (!clientInfoList.contains(clientInfo)) {
        			clientInfoList.add(clientInfo);
        			List<String> singleList = jedis.mget(keys);
            		//遍历取出的元素，如果不为空则加入到string结果集中,如果为空则加入空字符串
            		for (int j = 0; j < singleList.size(); j++) {
            			String single = singleList.get(j);
    					if (!StringUtils.isBlank(single)) {
    						resultStringList.add(single);
    					}else{
    						resultStringList.add("{}");
    					}
    				}
				}
			}
        	//将结果集反序列化 生成结果返回
        	for (int i = 0; i < resultStringList.size(); i++) {
        		String cacheValue = resultStringList.get(i);
        		T t = JSON.parseObject(cacheValue, clazz);
        		resultList.add(t);
			}
		}
    	return resultList;
    }
    /**
     * 批量取缓存对象     
     * @param keyList 所有KEY的list集合
     * @param clazz 缓存对象类型
     * @return 返回一个缓存对象Map
     */
   	public <T> Map<String, T> mgetObjMap (List<ICacheKey> keyList,Class<T> clazz){
   		initOP(OP.READ);
    	List<String> clientInfoList = null;
    	List<Jedis> jedisList = null;
    	Map<String, List<String>> groupKeysMap =null;
    	Map<String, T> resultMap = null;
   		if (keyList!=null&&keyList.size()>0) {
   			resultMap = new HashMap<String,T>();
   			clientInfoList =  new ArrayList<String>();
   			jedisList = new ArrayList<Jedis>();
   			groupKeysMap = new HashMap<String, List<String>>();
   			//把KEY先分组
   			String keys [] = new String[keyList.size()];
        	for (int i = 0; i < keyList.size(); i++) {
    			keys[i]= keyList.get(i).getKey();
    		}
        	for (int i = 0; i < keys.length; i++) {
        		//取出每一个KEY所在节点的Jedis实例 
        		Jedis jedis = proxy.getCurrJedis().getShard(keys[i]);
        		String clientInfo = jedis.getClient().getHost()+jedis.getClient().getPort();
        		//判断是否有该分组，如果没有，创建新list存入，如果有则添加对应位置的KEY
        		List<String> subKeyList = groupKeysMap.get(clientInfo);
        		if (subKeyList==null) {
        			subKeyList = new ArrayList<String>();
				}
        		subKeyList.add(keys[i]);
        		groupKeysMap.put(clientInfo, subKeyList);
        		//保存此redis实例，过滤掉相同的服务器
        		if (!clientInfoList.contains(clientInfo)) {
        			clientInfoList.add(clientInfo);
        			jedisList.add(jedis);
        		}
        	}
        	//遍历redis实例的列表，从分组的map中取出在此服务器上的key
        	for (int i = 0; i < jedisList.size(); i++) {
				Jedis jedis = jedisList.get(i);
				String clientInfo = jedis.getClient().getHost()+jedis.getClient().getPort();
				List<String> subKeyList = groupKeysMap.get(clientInfo);
				String [] subKeysArray = new String[subKeyList.size()];
				subKeysArray = subKeyList.toArray(subKeysArray);
				List<String> subResultList = jedis.mget(subKeysArray);
				for (int j = 0; j < subResultList.size(); j++) {
        			String single = subResultList.get(j);
        			if (!StringUtils.isBlank(single)) {
        				T t = JSON.parseObject(single, clazz);
        				resultMap.put(subKeysArray[j], t);
					}else{
						resultMap.put(subKeysArray[j], null);
					}
				}
			}
   		}
   		return resultMap;
   	}
   	
    /**
     * 批量取缓存对象     
     * @param keyList 所有KEY的list集合
     * @param clazz 缓存对象类型
     * @return 返回一个缓存对象List集合
     */
    /**
     * 批量取缓存对象     
     * @param keyList 所有KEY的list集合
     * @param clazz 缓存对象类型
     * @return 返回一个缓存对象Map
     */
   	public  Map<String, String> mgetStrMap (List<ICacheKey> keyList){
   		initOP(OP.READ);
    	List<String> clientInfoList = null;
    	List<Jedis> jedisList = null;
    	Map<String, List<String>> groupKeysMap =null;
    	Map<String, String> resultMap = null;
   		if (keyList!=null&&keyList.size()>0) {
   			resultMap = new HashMap<String,String>();
   			clientInfoList =  new ArrayList<String>();
   			jedisList = new ArrayList<Jedis>();
   			groupKeysMap = new HashMap<String, List<String>>();
   			//把KEY先分组
   			String keys [] = new String[keyList.size()];
        	for (int i = 0; i < keyList.size(); i++) {
    			keys[i]= keyList.get(i).getKey();
    		}
        	for (int i = 0; i < keys.length; i++) {
        		//取出每一个KEY所在节点的Jedis实例 
        		Jedis jedis = proxy.getCurrJedis().getShard(keys[i]);
        		String clientInfo = jedis.getClient().getHost()+jedis.getClient().getPort();
        		//判断是否有该分组，如果没有，创建新list存入，如果有则添加对应位置的KEY
        		List<String> subKeyList = groupKeysMap.get(clientInfo);
        		if (subKeyList==null) {
        			subKeyList = new ArrayList<String>();
				}
        		subKeyList.add(keys[i]);
        		groupKeysMap.put(clientInfo, subKeyList);
        		//保存此redis实例，过滤掉相同的服务器
        		if (!clientInfoList.contains(clientInfo)) {
        			clientInfoList.add(clientInfo);
        			jedisList.add(jedis);
        		}
        	}
        	//遍历redis实例的列表，从分组的map中取出在此服务器上的key
        	for (int i = 0; i < jedisList.size(); i++) {
				Jedis jedis = jedisList.get(i);
				String clientInfo = jedis.getClient().getHost()+jedis.getClient().getPort();
				List<String> subKeyList = groupKeysMap.get(clientInfo);
				String [] subKeysArray = new String[subKeyList.size()];
				subKeysArray = subKeyList.toArray(subKeysArray);
				List<String> subResultList = jedis.mget(subKeysArray);
				for (int j = 0; j < subResultList.size(); j++) {
        			String single = subResultList.get(j);
        			if (!StringUtils.isBlank(single)) {
        				resultMap.put(subKeysArray[j], single);
					}else{
						resultMap.put(subKeysArray[j], "");
					}
				}
			}
   		}
   		return resultMap;
   	}
   	
    public long increase(ICacheKey key) {
    	initOP(OP.WRITE);
    	ShardedJedis jedis = proxy.getCurrJedis();    	    		
    	long cacheValue = jedis.incr(key.getKey()).longValue();
    	if(key.getExpirationTime()>-1){
    		jedis.expire(key.getKey(), key.getExpirationTime());
		} else if (key.getExpirationAtTime() > 0) {
			jedis.expireAt(key.getKey(), key.getExpirationAtTime());
    	}
    	return cacheValue;
    }

	public long decrBy(ICacheKey key, long value) {
		initOP(OP.WRITE);
		ShardedJedis jedis = proxy.getCurrJedis();
		long cacheValue = jedis.decrBy(key.getKey(), value);
		if (key.getExpirationTime() > -1) {
			jedis.expire(key.getKey(), key.getExpirationTime());
		} else if (key.getExpirationAtTime() > 0) {
			jedis.expireAt(key.getKey(), key.getExpirationAtTime());
		}
		return cacheValue;
	}
    
    /**
     * 如果在插入的过程用，参数中有的成员在Set中已经存在，该成员将被忽略，而其它成员仍将会被正常插入。如果执行该命令之前，该Key并不存在，该命令将会创建一个新的Set，此后再将参数中的成员陆续插入。如果该Key的Value不是Set类型，该命令将返回相关的错误信息。
     * @param key
     * @param members
     * @return
     */
    public long sadd(ICacheKey key,final String... members){
    	initOP(OP.WRITE);
    	ShardedJedis jedis = proxy.getCurrJedis();
    	return jedis.sadd(key.getKey(), members);
    }
    
    /**
     * 获取与该Key关联的Set中所有的成员。
     * @param key
     * @return
     */
    public Set<String> smembers(ICacheKey key){
    	initOP(OP.READ);
    	return  proxy.getCurrJedis().smembers(key.getKey());
    }  
    
    public long decr(ICacheKey key){
    	initOP(OP.WRITE);
    	ShardedJedis jedis = proxy.getCurrJedis();
    	//使用原key的过期时间
    	long result=jedis.decr(key.getKey()).longValue();
		if(key.getExpirationTime() != -1){
    		jedis.expire(key.getKey(), key.getExpirationTime());
		} else if (key.getExpirationAtTime() > 0) {
			jedis.expireAt(key.getKey(), key.getExpirationAtTime());
    	}
    	return result;
    	
    }
    
    public Set<String> hkeys(ICacheKey key){
    	initOP(OP.READ);
    	return proxy.getCurrJedis().hkeys(key.getKey());
    }

	@Override
	public void hashPut(ICacheKey hashKeyName, String key, Object value) {
		if(value.getClass().equals(String.class)){
			hashPutStr(hashKeyName, key,String.valueOf(value));
		}else{
			String toJsonStr=JSONObject.toJSONString(value);
			hashPutStr(hashKeyName, key, toJsonStr);
		}		
	}

	@Override
	public <T> T hashGet(ICacheKey hashKeyName, String key, Class<T> clazz) {
		T obj=null;
		String jsonResult= hashGet(hashKeyName, key);
		if(org.apache.commons.lang3.StringUtils.isNotBlank(jsonResult)){
			obj=JSON.parseObject(jsonResult, clazz);
		}
		return obj;
	}

	@Override
	public List<String> hashVals(ICacheKey key) {
		initOP(OP.READ);
    	return proxy.getCurrJedis().hvals(key.getKey());
	}

	@Override
	public void hashMSet(ICacheKey key, Map<String, String> values) {
		initOP(OP.WRITE);
		ShardedJedis jedis = proxy.getCurrJedis();
		jedis.hmset(key.getKey(), values);
		if (key.getExpirationTime() != -1) {
			jedis.expire(key.getKey(), key.getExpirationTime());
		} else if (key.getExpirationAtTime() > 0) {
			jedis.expireAt(key.getKey(), key.getExpirationAtTime());
		}
	}

	@Override
	public void hashDel(ICacheKey hashKeyName, String key) {
		initOP(OP.WRITE);
		proxy.getCurrJedis().hdel(hashKeyName.getKey(), key);
	}

	@Override
	public boolean hashExists(ICacheKey hashKeyName, String key) {
		initOP(OP.READ);
		return proxy.getCurrJedis().hexists(hashKeyName.getKey(), key);
	}

	@Override
	public long hashIncrBy(ICacheKey key, String field, long value) {
		initOP(OP.WRITE);
		ShardedJedis jedis = proxy.getCurrJedis();
		long cacheValue = jedis.hincrBy(key.getKey(), field, value);
		if (key.getExpirationTime() > -1) {
			jedis.expire(key.getKey(), key.getExpirationTime());
		} else if (key.getExpirationAtTime() > 0) {
			jedis.expireAt(key.getKey(), key.getExpirationAtTime());
		}
		return cacheValue;
	}

	@Override
	public long ttl(ICacheKey key) {
		initOP(OP.READ);
		return proxy.getCurrJedis().ttl(key.getKey());
	}

	@Override
	public void exprie(ICacheKey key) {
		initOP(OP.WRITE);
		proxy.getCurrJedis().expire(key.getKey(), key.getExpirationTime());
		
	}

	@Override
	public List<String> hashMGet(ICacheKey key, String... fields) {
		initOP(OP.READ);
		return proxy.getCurrJedis().hmget(key.getKey(), fields);
	}

	@Override
	public long decrBy(BaseCacheKey key, Long amount) {
    	initOP(OP.WRITE);
    	ShardedJedis jedis = proxy.getCurrJedis();    	    		
    	long cacheValue = jedis.decrBy(key.getKey(),amount);
    	if(key.getExpirationTime()>-1){
    		jedis.expire(key.getKey(), key.getExpirationTime());
		} else if (key.getExpirationAtTime() > 0) {
			jedis.expireAt(key.getKey(), key.getExpirationAtTime());
    	}
    	return cacheValue;
    }
}
