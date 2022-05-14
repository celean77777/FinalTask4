package ru.gb.storage.server;

import java.sql.*;

public class DBservices {

    private static Connection connection;
    private static Statement statement;

    private DBservices(){

    }


    public static void dbStart() {
        try {
            System.out.println("Подключаюсь к базе....");
            connection = DBconnect.getConnection();
            statement = connection.createStatement();

        } catch (SQLException e) {
            System.out.println("Не могу открыть соединение с базой");
            closeConnection();
            e.printStackTrace();
        }
    }

    public static void closeConnection(){
        if (statement != null){
            try {
                statement.close();
            } catch (SQLException e) {
                System.out.println("Соединение закрыто");
                e.printStackTrace();
            }
        }
        if (connection != null){
            try {
                connection.close();
            } catch (SQLException e) {
                System.out.println("Соединение закрыто");
                e.printStackTrace();
            }
        }
    }
    // Добавление нового клиента//////////////////////////////////////////////////////////////
    public static void createNewClient(String login, String password, String nick) {
        try {
            PreparedStatement ps = connection.prepareStatement("INSERT INTO clients (login, password, nick) VALUES (?, ?, ?);");
            ps.setString(1, login);
            ps.setString(2, password);
            ps.setString(3, nick);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("login или nick уже заняты");
        }

    }


    // Достаем все поля из таблицы////////////////////////////////////////////////////////////
    public static void readEx() throws SQLException {
        try (ResultSet rs = statement.executeQuery("SELECT * FROM clients;")) {
            while (rs.next()) {
                System.out.println(rs.getInt(1) + " " + rs.getString(2) + " " +
                        rs.getString(3) + " " + rs.getString(4));
            }
        }
    }


    public static int updateNick (String oldNick, String newNick){
        try {
            PreparedStatement ps = connection.prepareStatement("UPDATE clients SET nick = ? WHERE nick = ?;");
            ps.setString(1, newNick);
            ps.setString(2, oldNick);
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Уточните имя, которое хотите заменить");
            return 0;
        }

    }

    public static String getNickByLoginAndPass (String login, String password){
        String nick;
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM clients WHERE login =  ? AND password = ?");
            ps.setString(1, login);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            nick = rs.getString("nick");
            System.out.println(nick);

        }catch (SQLException e){
            e.printStackTrace();
            return null;
        }
        return nick;
    }

    public static Integer isNickBusy (String name){
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT id FROM clients WHERE nick = ?;");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            return rs.getInt("id");
        }catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    public static String getLoginByName (String name){
        String login;
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT login FROM clients WHERE nick = ?;");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            login = rs.getString("login");
            System.out.println(login);
        }catch (SQLException e){
            e.printStackTrace();
            return null;
        }
        return login;
    }



}