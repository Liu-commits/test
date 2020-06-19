package cn.itcast.hibernate.query;

import java.util.List;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.transform.Transformers;
import org.hibernate.type.LongType;
import org.junit.Test;

import cn.itcast.crm.domain.CstCustomer;
import cn.itcast.crm.domain.CustomResult;
import cn.itcast.crm.util.HibernateUtil;

public class Sql {
	
		//原始sql，结果集映射为hibernate管理的pojo，但是sql必须查询查询到pojo中所有的列
		@Test
		public void test1(){
			Session session1 = HibernateUtil.openSession();
			SQLQuery sqlQuery1 = session1.createSQLQuery("select * from cst_customer");
//			sqlQuery1.addEntity(CstCustomer.class);
		
			List list = sqlQuery1.list();
//			System.out.println(list);
			session1.close();
		}
		
	//原生sql，结果集映射为自定义vo
		@Test
		public void test2(){
			Session session1 = HibernateUtil.openSession();
			SQLQuery sqlQuery1 = session1.createSQLQuery("select count(*) count from cst_customer group by cust_level");
			
			//设置列的映射类型
			sqlQuery1.addScalar("count", LongType.INSTANCE);
			//将结果映射为自定义Vo
			sqlQuery1.setResultTransformer(Transformers.aliasToBean(CustomResult.class));
			//也可以映射为hibernate管理的pojo，但是sql必须查询查询到pojo中所有的列
			//sqlQuery1.addEntity(CustomResult.class);
			List list = sqlQuery1.list();
//			System.out.println(list);
			session1.close();
			
			
		}
}
