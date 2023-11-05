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
public class XxlRpcLoadBalanceLFUStrategy extends XxlRpcLoadBalance {

    private ConcurrentMap<String, HashMap<String, Integer>> jobLfuMap = new ConcurrentHashMap<String, HashMap<String, Integer>>();

    private long CACHE_VALID_TIME = 0;

    public String doRoute(String serviceKey, TreeSet<String> addressSet) {
        // cache clear
        if (System.currentTimeMillis() > CACHE_VALID_TIME) {
            jobLfuMap.clear();
            CACHE_VALID_TIME = System.currentTimeMillis() + 1000 * 60 * 60 * 24;
        }
        // lfu item init
        // Key sorting can be constructed using TreeMap+as an input parameter Compare; Value sorting can only be done through ArrayList temporarily;
        HashMap<String, Integer> lfuItemMap = jobLfuMap.get(serviceKey);
        if (lfuItemMap == null) {
            lfuItemMap = new HashMap<String, Integer>();
            // Avoid duplicate coverage
            jobLfuMap.putIfAbsent(serviceKey, lfuItemMap);
        }
        // put new
        for (String address : addressSet) {
            if (!lfuItemMap.containsKey(address) || lfuItemMap.get(address) > 1000000) {
                lfuItemMap.put(address, 0);
            }
        }
        // remove old
        List<String> delKeys = new ArrayList<>();
        for (String existKey : lfuItemMap.keySet()) {
            if (!addressSet.contains(existKey)) {
                delKeys.add(existKey);
            }
        }
        if (delKeys.size() > 0) {
            for (String delKey : delKeys) {
                lfuItemMap.remove(delKey);
            }
        }
        // load least userd count address
        List<Map.Entry<String, Integer>> lfuItemList = new ArrayList<Map.Entry<String, Integer>>(lfuItemMap.entrySet());
        Collections.sort(lfuItemList, new Comparator<Map.Entry<String, Integer>>() {

            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        Map.Entry<String, Integer> addressItem = lfuItemList.get(0);
        String minAddress = addressItem.getKey();
        addressItem.setValue(addressItem.getValue() + 1);
        return minAddress;
    }

    @Override
    public String route(String serviceKey, TreeSet<String> addressSet) {
        String finalAddress = doRoute(serviceKey, addressSet);
        return finalAddress;
    }
}
