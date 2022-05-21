package mySQL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class clearDB {
	private static clearDB instancclearDB=null;
	public static clearDB v() {
		if (instancclearDB ==null) {
			synchronized (clearDB.class) {
				if (instancclearDB==null) {
					instancclearDB=new clearDB();
				}
			}
		}
		return instancclearDB;
	}
    public  clearDB() {
		
	}
	public static void clearTable() {
		try {
			Connection connection=DBUtil.getConnection();
			String sql="truncate table Edge";
			Statement statement=connection.createStatement();
			int rSet=statement.executeUpdate(sql);
			if(rSet == 0) {
				System.out.println("clear Edge succeed");
			}else {
				System.out.println("clear Edge failed");
			}
			sql="truncate table Window";
			int rSet1=statement.executeUpdate(sql);
			if(rSet1 == 0) {
				System.out.println("clear Window succeed");
			}else {
				System.out.println("clear Window failed");
			}
			sql="truncate table Widget";
			int rSet2=statement.executeUpdate(sql);
			if(rSet2 == 0) {
				System.out.println("clear Widget succeed");
			}else {
				System.out.println("clear Widget failed");
			}
			DBUtil.closeStatement(statement);
			DBUtil.closeConnection(connection);
		} catch (SQLException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
    
	
	public static void main(String[] args) {
		//clear  table(Window & Widget & Edge ) tuple 
		clearTable();

	}

}
