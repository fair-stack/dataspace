package cn.cnic.dataspace.api.datax.admin.core.route.strategy;

import cn.cnic.dataspace.api.datax.admin.core.route.ExecutorRouter;
import cn.cnic.dataspace.api.datax.core.biz.model.ReturnT;
import cn.cnic.dataspace.api.datax.core.biz.model.TriggerParam;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * For each actuator corresponding to a single JOB, the one with the lowest usage frequency is selected first
 */
public class ExecutorRouteLFU extends ExecutorRouter {

    private static ConcurrentMap<Integer, HashMap<String, Integer>> jobLfuMap = new ConcurrentHashMap<Integer, HashMap<String, Integer>>();

    private static long CACHE_VALID_TIME = 0;

    public String route(int jobId, List<String> addressList) {
        // cache clear
        if (System.currentTimeMillis() > CACHE_VALID_TIME) {
            jobLfuMap.clear();
            CACHE_VALID_TIME = System.currentTimeMillis() + 1000 * 60 * 60 * 24;
        }
        // lfu item init
        // Key sorting can be constructed using TreeMap+as an input parameter Compare; Value sorting can only be done through ArrayList temporarily;
        HashMap<String, Integer> lfuItemMap = jobLfuMap.get(jobId);
        if (lfuItemMap == null) {
            lfuItemMap = new HashMap<String, Integer>();
            // Avoid duplicate coverage
            jobLfuMap.putIfAbsent(jobId, lfuItemMap);
        }
        // put new
        for (String address : addressList) {
            if (!lfuItemMap.containsKey(address) || lfuItemMap.get(address) > 1000000) {
                // Actively Random once during initialization to relieve initial pressure
                lfuItemMap.put(address, new Random().nextInt(addressList.size()));
            }
        }
        // remove old
        List<String> delKeys = new ArrayList<>();
        for (String existKey : lfuItemMap.keySet()) {
            if (!addressList.contains(existKey)) {
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
        return addressItem.getKey();
    }

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        String address = route(triggerParam.getJobId(), addressList);
        return new ReturnT<String>(address);
    }
}
