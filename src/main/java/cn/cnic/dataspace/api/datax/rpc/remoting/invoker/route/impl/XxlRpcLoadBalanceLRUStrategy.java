package cn.cnic.dataspace.api.datax.rpc.remoting.invoker.route.impl;

import cn.cnic.dataspace.api.datax.rpc.remoting.invoker.route.XxlRpcLoadBalance;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * lru
 *
 * @author xuxueli 2018-12-04
 */
public class XxlRpcLoadBalanceLRUStrategy extends XxlRpcLoadBalance {

    private ConcurrentMap<String, LinkedHashMap<String, String>> jobLRUMap = new ConcurrentHashMap<String, LinkedHashMap<String, String>>();

    private long CACHE_VALID_TIME = 0;

    public String doRoute(String serviceKey, TreeSet<String> addressSet) {
        // cache clear
        if (System.currentTimeMillis() > CACHE_VALID_TIME) {
            jobLRUMap.clear();
            CACHE_VALID_TIME = System.currentTimeMillis() + 1000 * 60 * 60 * 24;
        }
        // init lru
        LinkedHashMap<String, String> lruItem = jobLRUMap.get(serviceKey);
        if (lruItem == null) {
            /**
             * LinkedHashMap
             */
            lruItem = new LinkedHashMap<String, String>(16, 0.75f, true) {

                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    if (super.size() > 1000) {
                        return true;
                    } else {
                        return false;
                    }
                }
            };
            jobLRUMap.putIfAbsent(serviceKey, lruItem);
        }
        // put new
        for (String address : addressSet) {
            if (!lruItem.containsKey(address)) {
                lruItem.put(address, address);
            }
        }
        // remove old
        List<String> delKeys = new ArrayList<>();
        for (String existKey : lruItem.keySet()) {
            if (!addressSet.contains(existKey)) {
                delKeys.add(existKey);
            }
        }
        if (delKeys.size() > 0) {
            for (String delKey : delKeys) {
                lruItem.remove(delKey);
            }
        }
        // load
        String eldestKey = lruItem.entrySet().iterator().next().getKey();
        String eldestValue = lruItem.get(eldestKey);
        return eldestValue;
    }

    @Override
    public String route(String serviceKey, TreeSet<String> addressSet) {
        String finalAddress = doRoute(serviceKey, addressSet);
        return finalAddress;
    }
}
