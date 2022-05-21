package mySQL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import zzg.staticguimodel.Node;

public class WindowDB {
	private static WindowDB instancWindowDB=null;
	public static WindowDB v() {
		if (instancWindowDB ==null) {
			synchronized (WindowDB.class) {
				if (instancWindowDB==null) {
					instancWindowDB=new WindowDB();
				}
			}
		}
		return instancWindowDB;
	}
    public  WindowDB() {
		
	}
    
    public void insertWindow(Node node) {
    	try {
    		Connection conn = DBUtil.getConnection();
            String sql = "insert into window(ID,winName,type,optionsMenuID,contextMenuID,leftDrawerID,rightDrawerID,fragSizes,fragIDString,widgetSizes,isTest,permissions,isActivityTest) " +
                    "values (?,?,?,?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
    		preparedStatement.setLong(1, node.getId());
    		preparedStatement.setString(2, node.getName());
    		preparedStatement.setString(3, node.getnType().toString());
    		preparedStatement.setLong(4, node.getOptionsMenu());
    		preparedStatement.setLong(5, node.getContextMenu());
    		preparedStatement.setLong(6, node.getLeftDrawer());
    		preparedStatement.setLong(7, node.getRightDrawer());
    		preparedStatement.setInt(8, node.getFragmentsName().size());
    		preparedStatement.setString(9, node.getFragIDString());  
    		preparedStatement.setLong(10, node.getWidgets().size());
    		preparedStatement.setBoolean(11, node.isTest());
    		preparedStatement.setString(12, node.getPermissions());
    		preparedStatement.setInt(13, node.getIsActivityTest());
            int changeRows = preparedStatement.executeUpdate();
            if(changeRows > 0){
                System.out.println("insert Window " + node.getId() + " successfully");
            }
            DBUtil.closePreparedStatement(preparedStatement);
            DBUtil.closeConnection(conn);
		} catch (SQLException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
	}
}
