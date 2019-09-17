package org.jd.proxy.codec;

import io.netty.buffer.ByteBuf;

public class XORCodec extends ByteBufCodec {
    private final byte code;

    public XORCodec(byte code) {
        this.code = code;
    }

    @Override
    protected ByteBuf decode(ByteBuf buf) throws Exception {
        for (int i = buf.readerIndex(), iEnd = buf.writerIndex(); i < iEnd; i++) {
            buf.setByte(i, buf.getByte(i) ^ code);
        }
        return buf;
    }

    @Override
    protected ByteBuf encode(ByteBuf msg) throws Exception {
        return decode(msg);
    }
}
