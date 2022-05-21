package com.soal.PRBDroid.sqlUtils;


import com.soal.PRBDroid.LogUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EdgeDao {//
    public static EdgeDao instance=null;

    public EdgeDao() {
    }
    public static EdgeDao V(){
        if (instance==null){
            synchronized (EdgeDao.class){
                if (instance==null){
                    instance=new EdgeDao();
                }
            }
        }
        return instance;
    }

    public void getEdges(){
        //读取所有边，存入staticInfo的eedges里
        //连接数据库
        try {
            Connection conn =DBUtil.getConnection();
            if (conn==null){
                LogUtil.V().logSql("空啊");
            }
            String sql = "select * from edge";
            PreparedStatement preparedStatement=conn.prepareStatement(sql);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()){
                staticInfo.EDGE aedge=new staticInfo.EDGE();
                aedge.ID=resultSet.getLong("ID");
                aedge.edgeLabel=resultSet.getString("edgeLabel");
                aedge.srcID=resultSet.getLong("srcID");
                aedge.tgtID=resultSet.getLong("tgtID");
                aedge.widgetID=resultSet.getLong("widgetID");
                staticInfo.V().eedges.add(aedge);
            }
            DBUtil.closeResultset(resultSet);
            DBUtil.closePreparedStatement(preparedStatement);
            DBUtil.closeConnection(conn);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

}
