package org.jd.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.jd.proxy.codec.XORCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    static Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        byte code = 1;
        serverStart(8001, code);
        clientStart(8000, "127.0.0.1", 8001, code);
    }

    static final Bootstrap connector = new Bootstrap().group(new NioEventLoopGroup(10))
            .channel(NioSocketChannel.class).handler(new ChannelHandlerAdapter() {
                @Override
                public boolean isSharable() {
                    return true;
                }
            });

    static void clientStart(int port, String sHost, int sPort, byte code) {
        new ServerBootstrap().group(new NioEventLoopGroup(20)).channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel channel) {
                        WriteToHandler writeToHandler = new WriteToHandler().setOut(connector, sHost, sPort).setOutTo(channel);
                        channel.pipeline().addFirst(new XORCodec(code), writeToHandler);
                    }
                }).bind(port);
    }

    static void serverStart(int port, byte code) {
        new ServerBootstrap().group(new NioEventLoopGroup(20)).channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel channel) {
                        channel.pipeline().addFirst(new XORCodec(code), new ServerService(connector));
                    }
                }).bind(port);
    }
}
