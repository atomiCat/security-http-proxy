package org.jd.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.jd.proxy.codec.XORCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class Main {
    static Logger log = LoggerFactory.getLogger(Main.class);

    /**
     * 启动参数
     * -s 8001 99
     * 启动服务端，监听端口 8001 密码 99
     * -c 8000 99 127.0.0.1 8001
     * 启动客户端，监听端口 8001 密码 99 服务端地址 127.0.0.1 端口 8001
     */
    public static void main(String[] args) {
        try {
            int port = Integer.parseInt(args[1]);
            Bootstrap connector = new Bootstrap().group(new NioEventLoopGroup(10))
                    .channel(NioSocketChannel.class).handler(new ChannelHandlerAdapter() {
                        @Override
                        public boolean isSharable() {
                            return true;
                        }
                    });
            XORCodec codec = new XORCodec(args[2]);
            //server
            if (args[0].equals("-s")) {
                start(port, channel -> channel.pipeline().addFirst(codec, new ServerService(connector)))
                        .addListener(future -> log.info("服务端启动完毕 ，成功：{} 异常：{}", future.isSuccess(), future.cause()));
            }
            //client
            if (args[0].equals("-c")) {
                int serverPort = Integer.parseInt(args[4]);
                start(port, channel -> {
                    WriteToHandler writeToHandler = new WriteToHandler().setOut(connector, args[3], serverPort).setOutTo(channel);
                    channel.pipeline().addFirst(codec, writeToHandler);
                }).addListener(future -> log.info("客户端启动完毕 ，成功：{} 异常：{}", future.isSuccess(), future.cause()));
            }
        } catch (Exception e) {
            log.warn("启动失败:{}", e.getMessage());
            log.warn("启动服务端参数： -s 端口 密码");
            log.warn("启动客户端参数： -c 端口 密码 服务端地址 服务端端口");
        }

    }

    static ChannelFuture start(int port, Consumer<NioSocketChannel> channelInit) {
        return new ServerBootstrap().group(new NioEventLoopGroup(20)).channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel channel) {
                        channelInit.accept(channel);
                    }
                }).bind(port);
    }
}
