package ru.gb.storage.client;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import ru.gb.storage.common.message.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

public class FirstClientHandler extends SimpleChannelInboundHandler<Message> {
    private static String fileName;
    private RandomAccessFile accessFile;

    public static void setFileName(String fileName){
        FirstClientHandler.fileName = fileName;
    }

    private final Button readFileBtn;
    private final Button writeFileBtn;
    private final Button authBtn;
    private final Button connBtn;
    private final TextField loginField;
    private final TextField passField;
    private final ListView<String> clientFileView;
    private final ListView<String> serverFileView;
    private final ObservableList<String> clientFileList;
    private final ObservableList<String> serverFileList;
    private final Label infoLbl;

    public FirstClientHandler(Button readFileBtn, Button writeFileBtn, Button authBtn, Button connBtn
            , TextField loginField, TextField passField, ListView<String> clientFileView
            , ListView<String> serverFileView, ObservableList<String> clientFileList
            , Label infoLbl, ObservableList<String> serverFileList){
        this.readFileBtn = readFileBtn;
        this.writeFileBtn = writeFileBtn;
        this.authBtn = authBtn;
        this.connBtn = connBtn;
        this.loginField = loginField;
        this.passField = passField;
        this.clientFileView = clientFileView;
        this.serverFileView = serverFileView;
        this.clientFileList = clientFileList;
        this.infoLbl = infoLbl;
        this.serverFileList = serverFileList;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx){
        System.out.println(ctx.channel().toString());
        Client.setCh(ctx.channel());
        javafx.application.Platform.runLater(() -> {
            connBtn.setDisable(true);
            connBtn.setText("YouAreConnect");
            infoLbl.setText("You are connected");
            authBtn.setDisable(false);
        });
        System.out.println("Client connect");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {

        if (msg instanceof TextMessage) {
            TextMessage message = (TextMessage) msg;
            //Меняем состояние элементов формы в зависимости от успеха авторизации
            if (message.getText().startsWith("/authOk")){
                javafx.application.Platform.runLater(() -> {
                    authBtn.setDisable(true);
                    authBtn.setText("AuthOK");
                    infoLbl.setText("Authorisation is OK.... Chose files for transmit or receive...");
                    readFileBtn.setDisable(false);
                    writeFileBtn.setDisable(false);
                });
            }
            // Получаем список файлов от сервера
            if (message.getText().startsWith("/fileList")){
               updateServerFileList(message);
            }

            if (message.getText().startsWith("/transmitOk")){
                javafx.application.Platform.runLater(() -> infoLbl.setText("Файл отправлен"));
            }
        }

        if (msg instanceof FileRequestMessage){
            FileRequestMessage frm = (FileRequestMessage) msg;
            final File file = new File(frm.getPath());
            System.out.println(frm.getPath());
            accessFile = new RandomAccessFile("D:\\Client\\" + file, "r");
            javafx.application.Platform.runLater(() -> infoLbl.setText("Идет отправка файла на сервер..."));
            sendFile(ctx);
        }

        if (msg instanceof FileContentMessage) {
            javafx.application.Platform.runLater(() -> infoLbl.setText("Идет скачивание файла из сервера..."));
            FileContentMessage fileContentMessage = (FileContentMessage) msg;
            try (final RandomAccessFile accessFile = new RandomAccessFile("D:\\Client\\" + fileName, "rw")) {
                accessFile.seek(fileContentMessage.getStartPosition());
                accessFile.write(fileContentMessage.getContent());
                if (fileContentMessage.isLast()) {
                    javafx.application.Platform.runLater(() -> {
                        infoLbl.setText("Файл скачан");
                        clientFileList.addAll(fileName);
                    });
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private void sendFile(ChannelHandlerContext ctx) throws IOException {
        if (accessFile!=null){

                javafx.application.Platform.runLater(() -> infoLbl.setText(infoLbl.getText() + "."));

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

    private void updateServerFileList(TextMessage message){
        javafx.application.Platform.runLater(() -> {
            String[] fileList = message.getText().split("##");
            List<String> serverList = new ArrayList<>(Arrays.asList(fileList));
            serverFileList.setAll(serverList);
        });
    }

}
