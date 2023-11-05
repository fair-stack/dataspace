package cn.cnic.dataspace.api.datax.rpc.remoting.invoker.route;

import java.util.TreeSet;

/**
 * The machine addresses under the group are the same, and different JOBs are evenly hashed on different machines to ensure that the JOBs allocated by the machines under the group are average; And each JOB is scheduled to schedule one of the machines on a fixed basis;
 */
public abstract class XxlRpcLoadBalance {

    public abstract String route(String serviceKey, TreeSet<String> addressSet);
}
