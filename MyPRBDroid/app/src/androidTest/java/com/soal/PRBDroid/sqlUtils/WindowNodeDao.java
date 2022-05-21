package com.soal.PRBDroid.sqlUtils;

//import java.sql.Statement;
//import java.sql.Connection;
//import java.sql.PreparedStatement;

import com.soal.PRBDroid.LogUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class WindowNodeDao {
    public static WindowNodeDao instance=null;

    public WindowNodeDao() {
    }
    public static WindowNodeDao V(){
        if (instance==null){
            synchronized (WindowNodeDao.class){
                if (instance==null){
                    instance=new WindowNodeDao();
                }
            }
        }
        return instance;
    }
    public void getWindows(){
        Connection conn =DBUtil.getConnection();
        if (conn==null){
            LogUtil.V().logSql("空啊");
        }
        String sql = "select * from window";
        try {
            PreparedStatement preparedStatement=conn.prepareStatement(sql);
            ResultSet resultSet=preparedStatement.executeQuery();
            while (resultSet.next()){
                staticInfo.WINDOW awindow=new staticInfo.WINDOW();
                awindow.ID=resultSet.getLong("ID");
                awindow.winName=resultSet.getString("winName");
                awindow.type=resultSet.getString("type");
                awindow.optionsMenuID=resultSet.getLong("optionsMenuID");
                awindow.contextMenuID=resultSet.getLong("contextMenuID");
                awindow.leftDrawerID=resultSet.getLong("leftDrawerID");
                awindow.rightDrawerID=resultSet.getLong("rightDrawerID");
                awindow.fragSizes=resultSet.getInt("fragSizes");
                awindow.fragIDString=resultSet.getString("fragIDString");
                awindow.widgetSizes=resultSet.getInt("widgetSizes");
                awindow.isTest=resultSet.getBoolean("isTest");
                awindow.permissions=resultSet.getString("permissions");
                awindow.isActivityTest=resultSet.getInt("isActivityTest");
                staticInfo.V().wwindows.add(awindow);
            }
            DBUtil.closeResultset(resultSet);
            DBUtil.closePreparedStatement(preparedStatement);
            DBUtil.closeConnection(conn);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
