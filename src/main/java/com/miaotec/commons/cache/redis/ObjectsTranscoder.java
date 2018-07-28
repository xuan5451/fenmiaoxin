/**
 * 
 */
package com.miaotec.commons.cache.redis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author zhangyonghui
 *
 */
public class ObjectsTranscoder {
	private static final Logger logger = LogManager.getLogger(RedisManagerProxy.class);

	public static byte[] serialize(List<String> serList) {
		if (serList == null || serList.size()<1) {
			return null;
		}
		byte[] rv = null;
		ByteArrayOutputStream bos = null;
		ObjectOutputStream os = null;
		try {
			bos = new ByteArrayOutputStream();
			os = new ObjectOutputStream(bos);
			for (String obj : serList) {
				os.writeObject(obj);
			}
			os.writeObject(null);
			os.close();
			bos.close();
			rv = bos.toByteArray();
		} catch (IOException e) {
			logger.error("Caught IOException encoding %s",serList, e);
		} finally {
			close(os);
			close(bos);
		}
		return rv;
	}

	public static List<String> deserialize(byte[] in) {
		List<String> list = new ArrayList<String>();
		ByteArrayInputStream bis = null;
		ObjectInputStream is = null;
		try {
			if (in != null) {
				bis = new ByteArrayInputStream(in);
				is = new ObjectInputStream(bis);
				while (true) {
					String obj = (String) is.readObject();
					if (obj == null) {
						break;
					} else {
						list.add(obj);
					}
				}
				is.close();
				bis.close();
			}
		} catch (IOException e) {
			logger.error("Caught IOException decoding %d bytes of data",in == null ? 0 : in.length, e);
		} catch (ClassNotFoundException e) {
			logger.error("Caught CNFE decoding %d bytes of data", in == null ? 0 : in.length, e);
		} finally {
			close(is);
			close(bis);
		}
		return list;
	}

	public static void close(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (Exception e) {
				logger.error("Unable to close %s", closeable, e);
			}
		}
	}
	
	public static void main(String[] args) {
		List<String> strlist = new ArrayList<String>();
		strlist.add("test1");
		strlist.add("test2");
		strlist.add("test3");
		byte[] serBytes = ObjectsTranscoder.serialize(strlist);
		List<String> desList = ObjectsTranscoder.deserialize(serBytes);
		for(Object o : desList){
			System.out.println((String)o);
		}
	}
}
