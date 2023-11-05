package cn.cnic.dataspace.api.datax.rpc.remoting.net.impl.netty.client;

import cn.cnic.dataspace.api.datax.rpc.remoting.invoker.XxlRpcInvokerFactory;
import cn.cnic.dataspace.api.datax.rpc.remoting.net.common.ConnectClient;
import cn.cnic.dataspace.api.datax.rpc.remoting.net.impl.netty.codec.NettyDecoder;
import cn.cnic.dataspace.api.datax.rpc.remoting.net.impl.netty.codec.NettyEncoder;
import cn.cnic.dataspace.api.datax.rpc.remoting.net.params.Beat;
import cn.cnic.dataspace.api.datax.rpc.remoting.net.params.XxlRpcRequest;
import cn.cnic.dataspace.api.datax.rpc.remoting.net.params.XxlRpcResponse;
import cn.cnic.dataspace.api.datax.rpc.serialize.Serializer;
import cn.cnic.dataspace.api.datax.rpc.util.IpUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.concurrent.TimeUnit;

/**
 * netty pooled client
 *
 * @author xuxueli
 */
public class NettyConnectClient extends ConnectClient {

    private EventLoopGroup group;

    private Channel channel;

    @Override
    public void init(String address, final Serializer serializer, final XxlRpcInvokerFactory xxlRpcInvokerFactory) throws Exception {
        final NettyConnectClient thisClient = this;
        Object[] array = IpUtil.parseIpPort(address);
        String host = (String) array[0];
        int port = (int) array[1];
        this.group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {

            @Override
            public void initChannel(SocketChannel channel) throws Exception {
                // beat N, close if fail
                // beat N, close if fail
                channel.pipeline().addLast(new IdleStateHandler(0, 0, Beat.BEAT_INTERVAL, TimeUnit.SECONDS)).addLast(new NettyEncoder(XxlRpcRequest.class, serializer)).addLast(new NettyDecoder(XxlRpcResponse.class, serializer)).addLast(new NettyClientHandler(xxlRpcInvokerFactory, thisClient));
            }
        }).option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_KEEPALIVE, true).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
        this.channel = bootstrap.connect(host, port).sync().channel();
        // valid
        if (!isValidate()) {
            close();
            return;
        }
        logger.debug(">>>>>>>>>>> xxl-rpc netty client proxy, connect to server success at host:{}, port:{}", host, port);
    }

    @Override
    public boolean isValidate() {
        if (this.channel != null) {
            return this.channel.isActive();
        }
        return false;
    }

    @Override
    public void close() {
        if (this.channel != null && this.channel.isActive()) {
            // if this.channel.isOpen()
            this.channel.close();
        }
        if (this.group != null && !this.group.isShutdown()) {
            this.group.shutdownGracefully();
        }
        logger.debug(">>>>>>>>>>> xxl-rpc netty client close.");
    }

    @Override
    public void send(XxlRpcRequest xxlRpcRequest) throws Exception {
        this.channel.writeAndFlush(xxlRpcRequest).sync();
    }
}
