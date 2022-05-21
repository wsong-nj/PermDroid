package com.soal.PRBDroid.sqlUtils;

import com.soal.PRBDroid.LogUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class WidgetDao {
    public static WidgetDao instance=null;

    public WidgetDao() {
    }
    public static WidgetDao V(){
        if (instance==null){
            synchronized (WidgetDao.class){
                if (instance==null){
                    instance=new WidgetDao();
                }
            }
        }
        return instance;
    }

    public void getWidgets(){
        Connection conn =DBUtil.getConnection();
        if (conn==null){
            LogUtil.V().logSql("空啊");
        }
        String sql = "select * from widget";
        try {
            PreparedStatement preparedStatement=conn.prepareStatement(sql);
            ResultSet resultSet=preparedStatement.executeQuery();
            while (resultSet.next()){
                staticInfo.WIDGET awidget=new staticInfo.WIDGET();
                awidget.ID=resultSet.getLong("ID");
                awidget.winID=resultSet.getLong("winID");
                awidget.winName=resultSet.getString("winName");
                awidget.type=resultSet.getString("type");
                awidget.resID=resultSet.getInt("resID");
                awidget.resName=resultSet.getString("resName");
                awidget.text=resultSet.getString("text");
                awidget.eventType=resultSet.getString("eventType");
                awidget.subMenuID=resultSet.getInt("subMenuID");
                awidget.itemIDString=resultSet.getString("itemIDString");
                awidget.itemID=resultSet.getInt("itemID");
                awidget.isWidgetTest=resultSet.getBoolean("isWidgetTest");
                awidget.permissions=resultSet.getString("permissions");
                staticInfo.V().wwidgets.add(awidget);
            }
            DBUtil.closeResultset(resultSet);
            DBUtil.closePreparedStatement(preparedStatement);
            DBUtil.closeConnection(conn);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
