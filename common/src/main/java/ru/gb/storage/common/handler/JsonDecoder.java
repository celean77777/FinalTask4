package ru.gb.storage.common.handler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.gb.storage.common.message.Message;

import java.util.List;
public class JsonDecoder extends MessageToMessageDecoder<ByteBuf> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        final byte[] bytes = ByteBufUtil.getBytes(msg);
        Message message = OBJECT_MAPPER.readValue(bytes, Message.class);
        out.add(message);
    }
}