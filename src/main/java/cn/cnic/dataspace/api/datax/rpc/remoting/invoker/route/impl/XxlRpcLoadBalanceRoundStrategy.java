package cn.cnic.dataspace.api.datax.rpc.remoting.invoker.route.impl;

import cn.cnic.dataspace.api.datax.rpc.remoting.invoker.route.XxlRpcLoadBalance;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * round
 *
 * @author xuxueli 2018-12-04
 */
public class XxlRpcLoadBalanceRoundStrategy extends XxlRpcLoadBalance {

    private ConcurrentMap<String, Integer> routeCountEachJob = new ConcurrentHashMap<String, Integer>();

    private long CACHE_VALID_TIME = 0;

    private int count(String serviceKey) {
        // cache clear
        if (System.currentTimeMillis() > CACHE_VALID_TIME) {
            routeCountEachJob.clear();
            CACHE_VALID_TIME = System.currentTimeMillis() + 24 * 60 * 60 * 1000;
        }
        // count++
        Integer count = routeCountEachJob.get(serviceKey);
        // Actively Random once during initialization to relieve initial pressure
        count = (count == null || count > 1000000) ? (new Random().nextInt(100)) : ++count;
        routeCountEachJob.put(serviceKey, count);
        return count;
    }

    @Override
    public String route(String serviceKey, TreeSet<String> addressSet) {
        // arr
        String[] addressArr = addressSet.toArray(new String[addressSet.size()]);
        // round
        String finalAddress = addressArr[count(serviceKey) % addressArr.length];
        return finalAddress;
    }
}
