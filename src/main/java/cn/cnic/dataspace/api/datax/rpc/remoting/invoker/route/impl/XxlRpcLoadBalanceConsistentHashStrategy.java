package cn.cnic.dataspace.api.datax.rpc.remoting.invoker.route.impl;

import cn.cnic.dataspace.api.datax.rpc.remoting.invoker.route.XxlRpcLoadBalance;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Consument hash
 */
public class XxlRpcLoadBalanceConsistentHashStrategy extends XxlRpcLoadBalance {

    private int VIRTUAL_NODE_NUM = 5;

    /**
     * Get hash code on 2 ^ 32 ring (MD5 hash calculation)
     */
    private long hash(String key) {
        // md5 byte
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
        md5.reset();
        byte[] keyBytes = null;
        try {
            keyBytes = key.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unknown string :" + key, e);
        }
        md5.update(keyBytes);
        byte[] digest = md5.digest();
        // hash code, Truncate to 32-bits
        long hashCode = ((long) (digest[3] & 0xFF) << 24) | ((long) (digest[2] & 0xFF) << 16) | ((long) (digest[1] & 0xFF) << 8) | (digest[0] & 0xFF);
        long truncateHashCode = hashCode & 0xffffffffL;
        return truncateHashCode;
    }

    public String doRoute(String serviceKey, TreeSet<String> addressSet) {
        // ------A1------A2-------A3------
        // -----------J1------------------
        TreeMap<Long, String> addressRing = new TreeMap<Long, String>();
        for (String address : addressSet) {
            for (int i = 0; i < VIRTUAL_NODE_NUM; i++) {
                long addressHash = hash("SHARD-" + address + "-NODE-" + i);
                addressRing.put(addressHash, address);
            }
        }
        long jobHash = hash(serviceKey);
        SortedMap<Long, String> lastRing = addressRing.tailMap(jobHash);
        if (!lastRing.isEmpty()) {
            return lastRing.get(lastRing.firstKey());
        }
        return addressRing.firstEntry().getValue();
    }

    @Override
    public String route(String serviceKey, TreeSet<String> addressSet) {
        String finalAddress = doRoute(serviceKey, addressSet);
        return finalAddress;
    }
}
