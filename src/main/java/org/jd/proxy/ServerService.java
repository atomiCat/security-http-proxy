package org.jd.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.http.HttpObjectDecoder;
import io.netty.handler.codec.http.HttpObjectEncoder;
import io.netty.util.CharsetUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * ====================https============================
 * CONNECT www.qq.com:443 HTTP/1.1[\r][\n]
 * Host: www.qq.com[\r][\n]
 * User-Agent: Apache-HttpClient/4.5.6 (Java/1.8.0_181)[\r][\n]
 * [\r][\n]
 * ====================http============================
 * GET http://www.qq.com/ HTTP/1.1
 * Host: www.qq.com
 * Proxy-Connection: Keep-Alive
 * User-Agent: Apache-HttpClient/4.5.6 (Java/1.8.0_181)
 */
public class ServerService extends LineBasedFrameDecoder {
    static final Logger log = LoggerFactory.getLogger(ServerService.class);
    private Bootstrap connector;

    public ServerService(Bootstrap connector) {
        super(1024);
        this.connector = connector;
    }
    ArrayList<String> headers = new ArrayList<>(4);

    @Override
    protected Object decode(ChannelHandlerContext client, ByteBuf buffer) throws Exception {
        Object decode = super.decode(client, buffer);
        if (decode == null)
            return null;
        String s = ((ByteBuf) decode).toString(CharsetUtil.UTF_8);
        headers.add(s);
        String first = headers.get(0);
        if (first.startsWith("CONNECT")) {// CONNECT www.qq.com:443 HTTP/1.1
            log.info("https ===> {}", first);
            if (s.length() > 0)
                return decode;
            WriteToHandler writeToHandler = new WriteToHandler().setOut(connector, StringUtils.split(first)[1], 443).setOutTo(client.channel());
            client.pipeline().addLast(writeToHandler).remove(getClass());
            //先告诉客户端连接成功，再进行连接,避免数据等待
            client.channel().writeAndFlush(Unpooled.copiedBuffer("HTTP/1.1 200 Connection Established \r\n\r\n", CharsetUtil.UTF_8));
        } else if (headers.size() == 3) {
            log.info("http ====> {}", first);
            String[] head = StringUtils.split(first);//GET http://www.qq.com:80/index.html HTTP/1.1
            String url = head[1];// http://www.qq.com:80/index.html
            int i = url.indexOf('/', 7);
            StringBuilder sb = new StringBuilder();
            sb.append(head[0]).append(' ').append(url, i, url.length()).append(' ').append(head[2]).append("\r\n");
            sb.append(headers.get(1)).append("\r\n").append("Connection: keep-alive\r\n");
            WriteToHandler writeToHandler = new WriteToHandler(Unpooled.copiedBuffer(sb, CharsetUtil.UTF_8))
                    .setOut(connector, url.substring(7, i), 80).setOutTo(client.channel());
            client.pipeline().addLast(writeToHandler).remove(getClass());
        }
        return decode;
    }
}
