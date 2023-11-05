package cn.cnic.dataspace.api.datax.rpc.remoting.invoker.route;

import cn.cnic.dataspace.api.datax.rpc.remoting.invoker.route.impl.*;

/**
 * @author xuxueli 2018-12-04
 */
public enum LoadBalance {

    RANDOM(new XxlRpcLoadBalanceRandomStrategy()), ROUND(new XxlRpcLoadBalanceRoundStrategy()), LRU(new XxlRpcLoadBalanceLRUStrategy()), LFU(new XxlRpcLoadBalanceLFUStrategy()), CONSISTENT_HASH(new XxlRpcLoadBalanceConsistentHashStrategy());

    public final XxlRpcLoadBalance xxlRpcInvokerRouter;

    private LoadBalance(XxlRpcLoadBalance xxlRpcInvokerRouter) {
        this.xxlRpcInvokerRouter = xxlRpcInvokerRouter;
    }

    public static LoadBalance match(String name, LoadBalance defaultRouter) {
        for (LoadBalance item : LoadBalance.values()) {
            if (item.equals(name)) {
                return item;
            }
        }
        return defaultRouter;
    }
}
