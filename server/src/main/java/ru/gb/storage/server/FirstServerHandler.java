package ru.gb.storage.server;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import ru.gb.storage.common.message.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class FirstServerHandler extends SimpleChannelInboundHandler<Message> {
    private RandomAccessFile accessFile;
    private Boolean isAuth = false;
    private final TextMessage answerMessage = new TextMessage();
    private final TextMessage answerFileListMessage = new TextMessage();
    private String pathFromClient;
    private List<String> serverList;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("New active channel");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws IOException {
        if (msg instanceof TextMessage) {
            TextMessage message = (TextMessage) msg;
            System.out.println("incoming text message: " + message.getText());
        }

        // Авторизация нового клиента
        if (msg instanceof AuthMessage){
            AuthMessage authMessage = (AuthMessage) msg;
                    String nick = DBservices.getNickByLoginAndPass(authMessage.getLogin(), authMessage.getPassword());
            System.out.println("Имя от сервера " + nick);
                    if (nick != null) {
                        isAuth = true;
                        answerMessage.setText("/authOk " + nick);
                        // Список файлов в расшаренной папке со стороны Сервера
                        serverFileList(ctx);
                    }else {
                        answerMessage.setText("/authNotOk");
                    }
                    ctx.writeAndFlush(answerMessage);


        }

        // Если клиент хочит считать файл из сервера, то в запросе имя файла и направление - чтение.
        // Далее по стандартному пути, как в примере на лекции.
        // Если наоборот, записать на сервер, то в запросе имя файла и направление - запись. В этом случае сервер пересылает клиенту его же
        // FileRequestMessage. В хендлере клиента те же функции, что и на сервере (почти) и запрос отрабатывает как сервер, т.е. пересылает файл..
        if (msg instanceof FileRequestMessage){
            FileRequestMessage frm = (FileRequestMessage) msg;
            if (isAuth) {
                if (frm.getDirection()) {
                    final File file = new File("D:\\Server\\" + frm.getPath());
                    accessFile = new RandomAccessFile(file, "r");
                    sendFile(ctx);
                }else {
                    pathFromClient = frm.getPath();
                    ctx.writeAndFlush(frm);
                }
            }else{
                System.out.println("Не авторизованный пользователь пытается получить доступ");
            }
        }


        if (msg instanceof FileContentMessage) {
            FileContentMessage fileContentMessage = (FileContentMessage) msg;
            try (final RandomAccessFile accessFile = new RandomAccessFile("D:\\Server\\" + pathFromClient, "rw")) {
                System.out.println(fileContentMessage.getStartPosition());
                accessFile.seek(fileContentMessage.getStartPosition());
                accessFile.write(fileContentMessage.getContent());
                if (fileContentMessage.isLast()) {
                    serverFileList(ctx);
                    TextMessage fileTransmitOk = new TextMessage();
                    fileTransmitOk.setText("/transmitOk");
                    ctx.writeAndFlush(fileTransmitOk);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void sendFile(ChannelHandlerContext ctx) throws IOException {
        if (accessFile!=null){
            final byte[] fileContent;
            final long available = accessFile.length()-accessFile.getFilePointer();
            if(available > 64 * 1024){
                fileContent = new byte[64 * 1024];
            }else {
                fileContent = new byte[(int) available];
            }
            final FileContentMessage message = new FileContentMessage();
            message.setStartPosition(accessFile.getFilePointer());
            accessFile.read(fileContent);
            message.setContent(fileContent);
            final boolean last = accessFile.getFilePointer() == accessFile.length();
            message.setLast(last);
            ctx.channel().writeAndFlush(message).addListener((ChannelFutureListener) channelFuture -> {
                if(!last) {
                    sendFile(ctx);
                }
            });
            if (last){
                accessFile.close();
                accessFile=null;
            }
        }
    }

    private void serverFileList(ChannelHandlerContext ctx){
        try(Stream<Path> paths = Files.walk(Paths.get("D:\\Server"))) {
            serverList = paths
                    .filter(Files::isRegularFile)
                    .map(in -> in.getFileName().toString())
                    .collect(Collectors.toList());
        }catch (RuntimeException | IOException e){
            System.out.println("Проверте наличие файлов по указанному пути");
        }
        StringBuffer fileString = new StringBuffer();
        for (String o:
                serverList) {
            fileString.append(o + "##");
        }
        String outString = fileString.toString();
        answerFileListMessage.setText("/fileList " + "##" + outString);
        System.out.println("Список файлов от сервера " + answerFileListMessage.getText());
        ctx.writeAndFlush(answerFileListMessage);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws IOException {
        System.out.println("client disconnect");
        if (accessFile!=null){
            accessFile.close();
        }
    }
}
