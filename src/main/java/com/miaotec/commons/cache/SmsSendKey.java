package com.miaotec.commons.cache;

public class SmsSendKey implements ICacheKey {

	private String key       = "SmsSendKey";
    private String version   = "2017031113";
    private String seperator = "_";

    private String subKey;
    private int    expSeconds;


    public SmsSendKey(String subKey, int expSeconds) {
        this.subKey = subKey;
        this.expSeconds = expSeconds;
    }


    @Override
    public String getKey() {
        return String.format("%s_%s_%s", key, subKey, version);
    }

    @Override
    public int getExpirationTime() {
        
        return this.expSeconds;
    }


    @Override
    public Object getValueFromSource() {
        
        return null;
    }

    @Override
    public int getLocalCacheTime() {
        return 0;
    }


    @Override
    public long getExpirationAtTime() {
        return 0;
    }

}
