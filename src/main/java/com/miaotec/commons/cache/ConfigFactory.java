/**
 * 
 */
package com.miaotec.commons.cache;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author zhangyonghui
 * 
 * 配置文件加载
 *
 */
public class ConfigFactory {
	private static final Logger logger = LogManager.getLogger(ConfigFactory.class);
	private static Properties properties;
	private static Properties hiveProperties;
	/**
	 * 初始化配置文件
	 * @param configPath 配置文件绝对路径
	 */
	public static void init(String configPath){
		InputStream in = null;		
		try {
			properties = new Properties();
			in = new FileInputStream(configPath);
			properties.load(in);
			in.close();
		} catch (IOException e) {
			logger.error("CommonConfigFactory init error",e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					logger.error("CommonConfigFactory close InputStream error",e);
				}
			}

		}
	}
	
	/**
	 * 获取配置内容
	 * @param key
	 * @return
	 */
	public static String getConfigValue(String key) {
		String result="";
		try {
			result= properties.getProperty(key);
		} catch (Exception ex) {
			logger.error("CommonConfigFactory-getConfigValue error:",ex);
		}
		return result;
	}
}
