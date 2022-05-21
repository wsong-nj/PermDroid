package mySQL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import zzg.staticguimodel.MenuItem;
import zzg.staticguimodel.SubMenu;
import zzg.staticguimodel.Widget;

public class WidgetDB {
	private static WidgetDB instancWidgetDB=null;
	public static WidgetDB v() {
		if (instancWidgetDB ==null) {
			synchronized (WidgetDB.class) {
				if (instancWidgetDB==null) {
					instancWidgetDB=new WidgetDB();
				}
			}
		}
		return instancWidgetDB;
	}
    public  WidgetDB() {
		
	}
    public  void insertWidget(Widget widget) {
		try {
			String string = "insert into widget(ID,winID,winName,type,resID,resName,text,eventType,subMenuID,itemIDString,itemID,isWidgetTest,permissions) "
					+ "values(?,?,?,?,?,?,?,?,?,?,?,?,?)";
			Connection connection = DBUtil.getConnection();
			PreparedStatement preparedStatement = connection.prepareStatement(string);
			preparedStatement.setLong(1, widget.getId());
			preparedStatement.setLong(2, widget.getWinID());
			preparedStatement.setString(3, widget.getWinName());
			preparedStatement.setString(4, widget.getType());
			preparedStatement.setInt(5, widget.getResId());
			preparedStatement.setString(6, widget.getResName());
			preparedStatement.setString(7, widget.getText());
			preparedStatement.setString(8, widget.getType());
			preparedStatement.setInt(9, widget.getSubMenuID());
			preparedStatement.setString(10, widget.getItemIDString());
			preparedStatement.setInt(11, widget.getItemID());
			preparedStatement.setBoolean(12, widget.isTest());
			preparedStatement.setString(13, widget.getPermissions());
			int changeRows = preparedStatement.executeUpdate();
			if (changeRows > 0) {
				System.out.println("insert Widget " + widget.getId() + " successfully");
			}
			DBUtil.closePreparedStatement(preparedStatement);
            DBUtil.closeConnection(connection);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
