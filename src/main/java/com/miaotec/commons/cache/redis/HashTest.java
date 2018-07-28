package com.miaotec.commons.cache.redis;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import redis.clients.util.SafeEncoder;

import com.miaotec.commons.cache.BaseCacheKey;
import com.miaotec.commons.cache.CommonCache;
import com.miaotec.commons.cache.ConfigFactory;
import com.miaotec.commons.cache.ICacheKey;


public class HashTest {

	 public static void main(String[] args){
	    	ConfigFactory.init( args[0]);
	    	Integer i = Integer.valueOf(255);
	    	String s = Integer.toString(i, 2);
	    	System.out.println(s);
	    	ICacheKey key = new BaseCacheKey("zhujiatest");
	    	CommonCache.getInstance().put(key, "haha");
	    	System.out.println(CommonCache.getInstance().get(key));
	    	System.out.println(((RedisManager)CommonCache.getInstance()).getHost(key));
	    	System.out.println(hash("haha"));
	    	System.out.println(hash("hehe"));
	    	System.out.println(hash("hehe123"));
	    	
	    }
	    public static long hash(String key)
	    {
	        return hash(SafeEncoder.encode(key));
	    }

	    public static long hash(byte key[])
	    {
	        
	        MessageDigest md5 = null;
			try {
				md5 = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        md5.reset();
	        md5.update(key);
	        byte bKey[] = md5.digest();
	        long res = (long)(bKey[3] & 255) << 24 | (long)(bKey[2] & 255) << 16 | (long)(bKey[1] & 255) << 8 | (long)(bKey[0] & 255);
	        return res;
	    }

}
