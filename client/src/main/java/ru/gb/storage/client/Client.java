package ru.gb.storage.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import ru.gb.storage.common.handler.JsonDecoder;
import ru.gb.storage.common.handler.JsonEncoder;
import ru.gb.storage.common.message.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Client extends Application {
    private static Channel ch;
    private static List<String> clientList;

    public static void setCh(Channel ch) {
        Client.ch = ch;
    }

    private Button readFileBtn;
    private Button writeFileBtn;
    private Button authBtn;
    private Button connBtn;
    private TextField loginField;
    private TextField passField;
    private ListView<String> clientFileView;
    private ListView<String> serverFileView;
    private ObservableList<String> clientFileList;
    private ObservableList<String> serverFileList;
    private Label infoLbl;


    public void setClientFileList(){
        try(Stream<Path> paths = Files.walk(Paths.get("D:\\Client"))) {
            clientList = paths
                    .filter(Files::isRegularFile)
                    .map(in -> in.getFileName().toString())
                    .collect(Collectors.toList());
        }catch (RuntimeException | IOException e){
            System.out.println("Проверте наличие файлов по указанному пути");
        }
        clientFileList = FXCollections.observableArrayList(clientList);
    }

    @Override
    public void start(Stage stage) throws IOException {

    // Список файлов в расшаренной папке со стороны Клиента
       setClientFileList();

    // Подготовка сцены
        serverFileList = FXCollections.observableArrayList();
        serverFileView = new ListView<>(serverFileList);
        serverFileView.setPlaceholder(new Label("No access Server files"));
        serverFileView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        serverFileView.setPrefSize(350, 250);


        clientFileView = new ListView<>(clientFileList);
        clientFileView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        clientFileView.setPrefSize(350, 250);

        readFileBtn = new Button("Read >>");
        readFileBtn.setOnAction(event -> {
            int index = serverFileView.getSelectionModel().getSelectedIndex();
            if (index>=0){
                FileRequestMessage frm = new FileRequestMessage();
                frm.setPath(serverFileView.getItems().get(index));
                frm.setDirection(true);
                FirstClientHandler.setFileName(serverFileView.getItems().get(index));
                ch.writeAndFlush(frm);
            }
        });

        writeFileBtn = new Button("Write <<");
        writeFileBtn.setOnAction(event -> {
            int index = clientFileView.getSelectionModel().getSelectedIndex();
            if (index>=0){
                FileRequestMessage frm = new FileRequestMessage();
                frm.setPath(clientFileView.getItems().get(index));
                frm.setDirection(false);
                ch.writeAndFlush(frm);
            }
        });

        infoLbl = new Label("");
        FlowPane infoPane = new FlowPane(10, 10, infoLbl);

        readFileBtn.setDisable(true);
        writeFileBtn.setDisable(true);
        FlowPane middlePane = new FlowPane(10, 10, serverFileView, readFileBtn, writeFileBtn, clientFileView);
        middlePane.setPrefWrapLength(1024.0);

        loginField = new TextField();
        passField = new TextField();
        Label loginLabel = new Label(" Login: ");
        Label passLabel = new Label(" Password: ");
        authBtn = new Button("Authorisation");
        FlowPane buttonPane = new FlowPane(10, 10, loginLabel, loginField, passLabel, passField, authBtn);
        buttonPane.setPrefWrapLength(1024.0);
        authBtn.setDisable(true);

        TextField urlField = new TextField();
        TextField portField = new TextField();
        Label urlLabel = new Label(" URL: ");
        Label portLabel = new Label(" Port: ");
        connBtn = new Button("Connect");
        FlowPane connPane = new FlowPane(10, 10, urlLabel, urlField, portLabel, portField, connBtn);
        connPane.setPrefWrapLength(1024.0);

// Подключение к серверу
        connBtn.setOnAction(event -> {
            if (!urlField.getText().equals("")&&!portField.getText().equals("")){
                startNet(urlField.getText(), portField.getText());
            }else {
                infoLbl.setText("URL field or/and Port field is empty...");
            }
                });

// Авторизаци
        authBtn.setOnAction(event -> {
            if (!loginField.getText().equals("")&&!passField.getText().equals("")){
                AuthMessage authMessage = new AuthMessage();
                authMessage.setAuth(loginField.getText(), passField.getText());
                ch.writeAndFlush(authMessage);
            }
        });

        FlowPane root = new FlowPane(Orientation.VERTICAL, 10, 10, buttonPane, middlePane, connPane, infoPane);
        Scene scene = new Scene(root, 860, 480);
        stage.setScene(scene);
        stage.setTitle("Client");
        stage.show();
    }

    public void startNet(String url, String port) {
        Thread t = new Thread(() -> {
            final NioEventLoopGroup group = new NioEventLoopGroup(1);
            try {
                Bootstrap bootstrap = new Bootstrap()
                        .group(group)
                        .channel(NioSocketChannel.class)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ch.pipeline().addLast(
                                        new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 3, 0, 3),
                                        new LengthFieldPrepender(3),
                                        new JsonDecoder(),
                                        new JsonEncoder(),
                                        new FirstClientHandler(readFileBtn, writeFileBtn, authBtn, connBtn
                                                , loginField, passField, clientFileView
                                                , serverFileView, clientFileList, infoLbl, serverFileList)
                                );
                            }
                        });

                System.out.println("Client started");
                Channel channel = bootstrap.connect(url, Integer.parseInt(port)).sync().channel();
                channel.closeFuture().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                group.shutdownGracefully();
            }
        });
        t.start();
        //t.setDaemon(true);
    }

    public static void main(String[] args) {
        launch();
    }
}