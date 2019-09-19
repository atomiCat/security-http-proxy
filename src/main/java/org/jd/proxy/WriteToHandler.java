package org.jd.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WriteToHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger("");
    private ConcurrentLinkedQueue<ByteBuf> buffer = new ConcurrentLinkedQueue<>();

    public WriteToHandler(ByteBuf... headMsg) {
        if (headMsg != null) {
            for (ByteBuf msg : headMsg) {
                addToBuffer(msg);
            }
        }
    }

    private transient Channel out;

    private WriteToHandler setOut(Channel out) {
        Assert.isTrue(this.out == null && out != null, "禁止重复设置！");
        writeTo(buffer, out);
        this.out = out;
        out.flush();//setOut 时可能已经读取完毕
        return this;
    }

    private ChannelFuture connectFuture;

    /**
     * 连接成功后设置out
     *
     * @param b           连接器
     * @param host        远程地址 www.qq.com 或 www.qq.com:443
     * @param defaultPort 默认端口
     * @return
     */
    WriteToHandler setOut(Bootstrap b, String host, int defaultPort) {
        int i = host.indexOf(':');
        connectFuture = i < 0 ? b.connect(host, defaultPort) : b.connect(host.substring(0, i), Integer.parseInt(host.substring(i + 1)));
        connectFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                this.setOut(future.channel());//客户端数据发往服务器
            } else {
//                throw new RuntimeException(future.cause());
                log.warn("连接失败 {}:{}  {}", host, defaultPort, future.cause().getMessage());
                ctx.close().addListener(f -> {
                    while (!buffer.isEmpty())
                        buffer.poll().release();
                });
            }
        });
        return this;
    }

    /**
     * 设置完 out 后，将从out读到的数据复制到 to 中
     *
     * @param to
     * @return
     */
    public WriteToHandler setOutTo(Channel to, ChannelHandler... first) {
        connectFuture.addListener((ChannelFutureListener) future -> {
            future.channel().pipeline().addLast(new WriteToHandler().setOut(to)).addFirst(first);
        });
        return this;
    }

    private void writeTo(Queue<ByteBuf> data, Channel out) {
        while (!data.isEmpty()) {
//            Assert.isTrue(data.peek().readableBytes() > 0, "可读取数量为0");
            out.write(data.poll());
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (out == null) {
            addToBuffer((ByteBuf) msg);
        } else {
            writeTo(buffer, out);
            out.write(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        if (out != null)
            out.flush();
    }

    private void addToBuffer(ByteBuf msg) {
        if (msg.readableBytes() > 0)
            buffer.offer(msg);
        else log.warn("可读取数量为0");
    }

    ChannelHandlerContext ctx;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        super.channelRegistered(ctx);
    }
}
