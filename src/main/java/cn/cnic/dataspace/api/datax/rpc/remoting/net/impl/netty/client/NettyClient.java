package cn.cnic.dataspace.api.datax.rpc.remoting.net.impl.netty.client;

import cn.cnic.dataspace.api.datax.rpc.remoting.net.Client;
import cn.cnic.dataspace.api.datax.rpc.remoting.net.common.ConnectClient;
import cn.cnic.dataspace.api.datax.rpc.remoting.net.params.XxlRpcRequest;

/**
 * netty client
 *
 * @author xuxueli 2015-11-24 22:25:15
 */
public class NettyClient extends Client {

    private Class<? extends ConnectClient> connectClientImpl = NettyConnectClient.class;

    @Override
    public void asyncSend(String address, XxlRpcRequest xxlRpcRequest) throws Exception {
        ConnectClient.asyncSend(xxlRpcRequest, address, connectClientImpl, xxlRpcReferenceBean);
    }
}
