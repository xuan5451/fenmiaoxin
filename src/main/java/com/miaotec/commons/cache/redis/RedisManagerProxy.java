/**
 * 
 */
package com.miaotec.commons.cache.redis;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;

import com.miaotec.commons.util.CommonConfigFactory;


/**
 * @author zhangyonghui
 *
 */
public class RedisManagerProxy implements MethodInterceptor{
	private static final Logger logger = LogManager.getLogger(RedisManagerProxy.class);
	RedisClientPool  jedisPool;
    private static final ThreadLocal<ShardedJedis> currJedis = new ThreadLocal<ShardedJedis>();        
    private static final Map<String,OP> methodNameMap = new HashMap<String,OP>();
    static{
		methodNameMap.put("del", OP.WRITE);
		methodNameMap.put("put", OP.WRITE);
		methodNameMap.put("get", OP.READ);
		methodNameMap.put("getByte", OP.READ);
		methodNameMap.put("exists", OP.READ);
		methodNameMap.put("existsByte", OP.READ);
		methodNameMap.put("push", OP.WRITE);
		methodNameMap.put("pull", OP.READ);
		methodNameMap.put("length", OP.READ);
		methodNameMap.put("popByte", OP.READ);
		methodNameMap.put("pop", OP.READ);
		methodNameMap.put("hashPut", OP.WRITE);
		methodNameMap.put("hashLength", OP.READ);
		methodNameMap.put("hashGet", OP.READ);
		methodNameMap.put("hashByteLength", OP.READ);
		methodNameMap.put("hashByteGet", OP.READ);
		methodNameMap.put("flush", OP.WRITE);
		methodNameMap.put("flushByte", OP.WRITE);
		/**
		 * 新定义的几个方法
		 */
		methodNameMap.put("getList", OP.READ);
		methodNameMap.put("getStr", OP.READ);
		methodNameMap.put("getObj", OP.READ);
		methodNameMap.put("getMap", OP.READ);
		methodNameMap.put("mgetObjList", OP.READ);
		methodNameMap.put("mgetObjMap", OP.READ);
		methodNameMap.put("incr", OP.WRITE);
		methodNameMap.put("sadd", OP.WRITE);
		methodNameMap.put("smembers", OP.READ);
		methodNameMap.put("decr", OP.WRITE);
		methodNameMap.put("hkeys", OP.READ);
		methodNameMap.put("expire", OP.WRITE);		
	}
	public RedisManagerProxy(String ips) {		
		Config config = loadPoolConfig();
		if (ips == null){
			if (CommonConfigFactory.getConfigValue("redis.ip") != null){
				ips = CommonConfigFactory.getConfigValue("redis.ip");
			}else{
				ips = RedisManager.DEFAULTIPFORMAT;
			}
		}
		logger.info("redis集群初始化地址为"+ips);
    	String[] ip = ips.split(",");    	
    	List<JedisShardInfo> masterList = new LinkedList<JedisShardInfo>();
    	List<JedisShardInfo> slaveList = new LinkedList<JedisShardInfo>();
    	JedisShardInfo jedisInfo = null;
    	try{
	    	for (int i=0;i<ip.length;i++){
	    		String[] ipinfo = ip[i].split(":");
	    		if (ipinfo.length >= 4){
	    			jedisInfo = new JedisShardInfo(ipinfo[0],Integer.valueOf(ipinfo[1]),ipinfo[3]);
	    			if(ipinfo.length == 5){
	    				jedisInfo.setPassword(ipinfo[4]);
	    			}
	    			if("0".equals(ipinfo[2])){
	    				masterList.add(jedisInfo);
	    			}
	    			else{
	    				slaveList.add(jedisInfo);
	    			}
	    		}
    	    }
    	}catch(Exception e){
    		logger.error("ip格式不对，示例:"+RedisManager.DEFAULTIPFORMAT);
    		System.exit(-1);
    	}
//    	if (list.size()==0){
//	    	logger.error("ip格式不对，示例:"+RedisManager.DEFAULTIPFORMAT);
//	    	System.exit(-1);
//	    }
    	jedisPool  = new RedisClientPool(config,masterList,slaveList);
	}
	
	public void gcJedis(Jedis jedis) {
		jedisPool.returnResource(jedis);
	}
	public ShardedJedis createJedis(){
		return jedisPool.getResource();
	}
	public ShardedJedis getCurrJedis(){
		return currJedis.get();
	}	
	
	private int toInt(byte b) {
			return b >= 0 ? (int)b : (int)(b - 256);
	}
	
	RedisManager target;
	public Object getProxy(Object target){
		 this.target = (RedisManager) target;
		 Enhancer enhancer = new Enhancer();  
		 enhancer.setSuperclass(target.getClass());  
		 enhancer.setCallback(this); 
	     return enhancer.create(); 	
	}
	
	@Override
	public Object intercept(Object arg0, Method arg1, Object[] arg2,
			MethodProxy methodPxoxy) throws Throwable {
		try{
//		    if (target.cop.get()==OP.READ){
//		    	currJedis.set(jedisPool.getSlaveResource());
//		    	logger.info("now get read jedis from:"+currJedis.get());
//		    }else{
//		    	currJedis.set(jedisPool.getResource());
//		    	logger.info("now get jedis from:"+currJedis.get());
//		    }
			OP op = methodNameMap.get(methodPxoxy.getSignature().getName());
			
			//由于金融数据实时性要求比较高,改为读写都由主库负责			
			currJedis.set(jedisPool.getResource());
			
			Object oj = methodPxoxy.invoke(target, arg2);
			return oj;
		}catch(Throwable e){
			if (e instanceof InvocationTargetException){
				e = ((InvocationTargetException) e).getTargetException();
			}
			logger.error(e.getMessage(),e);
		}finally{			
			jedisPool.returnResource(currJedis.get());//ShardedJedis			
			currJedis.remove();
		}
		return null;
	}
	public RedisClientPool getJedisPool() {
		return jedisPool;
	}
	public void setJedisPool(RedisClientPool jedisPool) {
		this.jedisPool = jedisPool;
	}
	
//	public void loadResources(){		
//		try {
//			InputStream is = Resources.getResourceAsStream("redisservice.properties", this.getClass());
//			if (is != null){
//				resources = new Properties();
//				resources.load(is);
//			}
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	
	public Config loadPoolConfig(){
		Config config = new Config();   	
    	try {		
			//设置默认
			config.maxActive = CommonConfigFactory.getConfigValue("maxActive") == null ? 50 : Integer.valueOf(CommonConfigFactory.getConfigValue("maxActive"));
			config.maxIdle = CommonConfigFactory.getConfigValue("maxIdle") == null ? 5 : Integer.valueOf(CommonConfigFactory.getConfigValue("maxIdle"));
			config.maxWait = CommonConfigFactory.getConfigValue("maxWait") == null ? 5000 : Integer.valueOf(CommonConfigFactory.getConfigValue("maxWait"));
			config.testOnBorrow = CommonConfigFactory.getConfigValue("testOnBorrow") == null ? true : Boolean.valueOf(CommonConfigFactory.getConfigValue("testOnBorrow"));
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return config;
	}	
}
