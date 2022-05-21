package mySQL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import zzg.staticguimodel.Edge;



public class EdgeDB {
	private static EdgeDB instancEdgeDB=null;
	public static EdgeDB v() {
		if (instancEdgeDB ==null) {
			synchronized (EdgeDB.class) {
				if (instancEdgeDB==null) {
					instancEdgeDB=new EdgeDB();
				}
			}
		}
		return instancEdgeDB;
	}
    public  EdgeDB() {
		
	}
	
	
	public void insertEdge(Edge edge){
        try{
            Connection conn = DBUtil.getConnection();
            String sql = "insert into edge(ID,edgeLabel,srcID,tgtID,widgetID) " +
                    "values (?,?,?,?,?)";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setLong(1,edge.getId());
            preparedStatement.setString(2,edge.getNote());
            preparedStatement.setLong(3,edge.getSrc().getId());
            preparedStatement.setLong(4, edge.getTgt().getId());
            if(edge.getWidget()!=null) {
            	preparedStatement.setLong(5,edge.getWidget().getId());
            }else {
            	preparedStatement.setLong(5,0);
            }
            
            int changeRows = preparedStatement.executeUpdate();
            if(changeRows > 0){
                System.out.println("insert TransitionEdge " + edge.getId() + " successfully");
            }
            DBUtil.closePreparedStatement(preparedStatement);
            DBUtil.closeConnection(conn);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
