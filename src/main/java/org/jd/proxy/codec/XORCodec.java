package org.jd.proxy.codec;

import io.netty.buffer.ByteBuf;

/**
 * 采用异或方式编码解码
 */
public class XORCodec extends ByteBufCodec {
    private final byte code;

    public XORCodec(byte code) {
        this.code = code;
    }

    @Override
    protected ByteBuf encode(ByteBuf buf) throws Exception {
        for (int i = buf.readerIndex(), iEnd = buf.writerIndex(); i < iEnd; i++) {
            buf.setByte(i, buf.getByte(i) ^ code);
        }
        return buf;
    }

    @Override
    protected ByteBuf decode(ByteBuf buf) throws Exception {
        return encode(buf);//异或编解码方式相同
    }
}
