package ru.gb.storage.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBconnect {

    private static Connection connection;
    private DBconnect() {

    }

    public static Connection getConnection() throws SQLException {
        if (connection == null){
            connection = DriverManager.getConnection("jdbc:sqlite:D:\\clientsDB.db");
        }
        return connection;
    }
}
