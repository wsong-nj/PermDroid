package com.soal.PRBDroid.sqlUtils;

import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class DBUtil {
    private final static String DRIVER = "com.mysql.jdbc.Driver";
    private final static String URL = "jdbc:mysql://192.168.153.2:3306/stg?useUnicode=true&characterEncoding=UTF-8&useSSL=false";
    private final static String USERNAME = "ysh";// ysh         2. ysh1
    private final static String PASSWORD = "ysh";// ysh       2. 123456

    //192.168.1.127
    //192.168.56.1
    //192.168.1.153
    public static Connection getConnection(){
        Connection connection = null;
        try{
            Class.forName(DRIVER);
            connection = DriverManager.getConnection(URL,USERNAME,PASSWORD);
//            Class.forName("com.mysql.jdbc.Driver");
//            Connection cn= DriverManager.getConnection("jdbc:mysql://192.168.56.1:3306/stg","ysh","ysh");
            Log.i("sql","connect successfully");
            //connection=cn;
        }catch(Exception e ){
            Log.i("sql", "connect failed");
            e.printStackTrace();
        }
        return connection;
    }

    public static void closeConnection(Connection connection) {
        try {
            if (connection != null)
                connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        connection = null;
    }
    public static void closePreparedStatement(PreparedStatement preparedStatement) {
        try {
            if (preparedStatement != null)
                preparedStatement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        preparedStatement = null;
    }
    public static void closeStatement(Statement statement){
        try{
            if(statement != null){
                statement.close();
            }

        }catch(Exception e){
            e.printStackTrace();
        }
        statement = null;
    }
    public static void closeResultset(ResultSet resultSet){
        try{
            if(resultSet != null)
                resultSet.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        resultSet = null;
    }
}