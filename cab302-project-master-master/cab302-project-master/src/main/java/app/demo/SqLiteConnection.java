package app.demo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SqLiteConnection {
    private static Connection instance = null;

    private SqLiteConnection(){}


    public static Connection getInstance() {
        if (instance == null) {
            try{
                instance = DriverManager.getConnection("jdbc:sqlite:contacts.db");
            }catch (SQLException e){
                e.printStackTrace();
            }
        }
        return instance;
    }
}

