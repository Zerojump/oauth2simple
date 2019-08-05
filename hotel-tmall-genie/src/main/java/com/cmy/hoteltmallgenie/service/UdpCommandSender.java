package com.cmy.hoteltmallgenie.service;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>@author chenmingyi
 * <p>@version 1.0
 * <p>date: 2019/8/4
 */
public class UdpCommandSender {
    private static final Logger LOG = LoggerFactory.getLogger(UdpCommandSender.class);

    private Channel channel;
    private EventLoopGroup group;
    private String host;
    private int port;
    private ExecutorService threadPool;

    public UdpCommandSender(String host, int port) {
        this.host = host;
        this.port = port;
        this.threadPool = Executors.newSingleThreadExecutor();
        init();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    private void init() {
        group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioDatagramChannel.class)
                    .handler(new RespHandler());

            channel = b.bind(0).sync().channel();

            threadPool.submit(() -> {
                try {
                    channel.closeFuture().await();
                    group.shutdownGracefully();
                } catch (InterruptedException e) {
                    LOG.error("init sender fail!",e);
                }
            });

        } catch (Exception e) {
            LOG.error("channel error when init udp sender", e);
        }
    }

    public void close() {
        LOG.info("channel is closing");
        group.shutdownGracefully();
        LOG.info("channel is closed");
    }

    public void send(String command) {
        send(host, port, command);
    }

    public void send(String host, int port, String command) {
        try {
            channel.writeAndFlush(new DatagramPacket(
                    Unpooled.copiedBuffer(command, CharsetUtil.UTF_8),
                    new InetSocketAddress(host, port))).sync();
            LOG.info("send [{}] -> <{}:{}>", command, host, port);
        } catch (InterruptedException e) {
            LOG.error(String.format("occur error when channel write! host:%s, port:%d, command:%s", host, port, command), e);
        }
    }

    private static class RespHandler extends SimpleChannelInboundHandler<DatagramPacket> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
            String body = packet.content().toString(CharsetUtil.UTF_8);
            //InetSocketAddress remote = (InetSocketAddress) ctx.channel().remoteAddress();
            LOG.info("msg from server[]:{}", body);
        }
    }
}
