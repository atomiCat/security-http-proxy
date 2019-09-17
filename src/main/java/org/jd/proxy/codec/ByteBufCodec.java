package org.jd.proxy.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

@ChannelHandler.Sharable
public abstract class ByteBufCodec extends ChannelDuplexHandler {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ctx.fireChannelRead(decode((ByteBuf) msg));
    }

    protected abstract ByteBuf decode(ByteBuf msg) throws Exception;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ctx.write(encode((ByteBuf) msg), promise);
    }

    protected abstract ByteBuf encode(ByteBuf msg) throws Exception;
}
