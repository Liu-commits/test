package demo;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;

import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleTypes;

import org.junit.Test;

public class TestOracle {
/*
 * create or replace procedure queryEmpInformation(eno in number,
                                                pename out varchar2,
                                                psal   out number,
                                                pjob   out varchar2)
 */
	@Test
	public void testProcedure(){
		//{call <procedure-name>[(<arg1>,<arg2>, ...)]}
		String sql = "{call queryEmpInformation(?,?,?,?)}";
		
		Connection conn = null;
		CallableStatement call = null;
		try {
			conn = JDBCUtils.getConnection();
			call = conn.prepareCall(sql);
			
			//����in��������ֵ
			call.setInt(1,7839);
			
			//����out����������
			call.registerOutParameter(2, OracleTypes.VARCHAR);
			call.registerOutParameter(3, OracleTypes.NUMBER);
			call.registerOutParameter(4, OracleTypes.VARCHAR);
			
			//ִ��
			call.execute();
			
			//���
			String name = call.getString(2);
			double sal = call.getDouble(3);
			String job = call.getString(4);
			
			System.out.println(name+"\t"+sal+"\t"+job);
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			JDBCUtils.release(conn, call, null);
		}
	}

/*
 * create or replace function queryEmpIncome(eno in number) 
return number
 */
	@Test
	public void testFunction(){
		//{?= call <procedure-name>[(<arg1>,<arg2>, ...)]}
		String sql = "{?=call queryEmpIncome(?)}";
		
		Connection conn = null;
		CallableStatement call = null;
		try {
			conn = JDBCUtils.getConnection();
			call = conn.prepareCall(sql);
			
			call.registerOutParameter(1, OracleTypes.NUMBER);
			call.setInt(2, 7839);
			
			//ִ��
			call.execute();
			
			//ȡ��������
			double income = call.getDouble(1);
			
			System.out.println(income);
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			JDBCUtils.release(conn, call, null);
		}		
	}


	@Test
	public void testCursor(){
		String sql = "{call mypackage.QUERYEMPLIST(?,?)}";
		
		Connection conn = null;
		CallableStatement call = null;
		ResultSet rs = null;
		try {
			conn = JDBCUtils.getConnection();
			call = conn.prepareCall(sql);
			
			//����in��������ֵ
			call.setInt(1,20);
			
			//����out����������
			call.registerOutParameter(2, OracleTypes.CURSOR);
			
			//ִ��
			call.execute();
			
			//ȡ�����
			rs = ((OracleCallableStatement)call).getCursor(2);
			while(rs.next()){
				//ȡ��һ��Ա��
				String name = rs.getString("ename");
				double sal = rs.getDouble("sal");
				System.out.println(name+"\t"+sal);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			JDBCUtils.release(conn, call, rs);
		}		
		
	}
}














