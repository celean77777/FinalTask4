package ru.gb.storage.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import ru.gb.storage.common.handler.JsonDecoder;
import ru.gb.storage.common.handler.JsonEncoder;

import java.sql.SQLException;

public class Server {
    private final int port;

    public static void main(String[] args) throws InterruptedException {
        new Server(9000).start();
    }

    public Server(int port) {
        this.port = port;
    }

    public void start() throws InterruptedException {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap server = new ServerBootstrap();
            server
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) //Используем серверную версию сокета
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) {
                            ch.pipeline().addLast(
                                    new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 3, 0, 3),
                                    new LengthFieldPrepender(3),
                                    new JsonDecoder(),
                                    new JsonEncoder(),
                                    new FirstServerHandler()
                            );
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            Channel channel = server.bind(port).sync().channel();
            System.out.println("Server started");

            // Подключение к Базе данных, проверка списка клиентов
            DBservices.dbStart();
            try {
                DBservices.readEx();
            } catch (SQLException e) {
                System.out.println("База данных отсутствует. Проверьте наличие базы");
                e.printStackTrace();
            }

            channel.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}