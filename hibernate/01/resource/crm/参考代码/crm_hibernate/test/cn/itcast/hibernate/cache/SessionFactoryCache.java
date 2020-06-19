package cn.itcast.hibernate.cache;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.boot.model.source.spi.HibernateTypeSource;
import org.hibernate.transform.Transformers;
import org.hibernate.type.LongType;
import org.hibernate.type.Type;
import org.junit.Test;

import cn.itcast.crm.domain.BaseDict;
import cn.itcast.crm.domain.CstCustomer;
import cn.itcast.crm.domain.CustomResult;
import cn.itcast.crm.util.HibernateUtil;

public class SessionFactoryCache {

	//get
	@Test
	public void test1(){
		//新开一个session
		Session session1 = HibernateUtil.openSession();
		//从数据库查询
		CstCustomer cstCustomer1 = session1.get(CstCustomer.class, 2l);
//		query.setCacheable(true);//设置使用查询缓存
//		List list = query.list();
//		System.out.println(cstCustomer1.getCstCustomerDetail().getCustAddress());
		session1.close();
		//新开一个session
		Session session2 = HibernateUtil.openSession();
		//从二级缓存取
		CstCustomer cstCustomer2 = session2.get(CstCustomer.class, 2l);
//		query.setCacheable(true);//设置使用查询缓存
//		List list2 = query.list();
		System.out.println(cstCustomer2);
		session2.close();
	}
	
	//hql
	@Test
	public void test2(){
		Session session1 = HibernateUtil.openSession();
		//客户来源字典查询
		Query query1 = session1.createQuery("from BaseDict b where b.dictTypeCode='002'");
		query1.setCacheable(true);//设置使用查询缓存
		List list = query1.list();
		System.out.println(list);
		session1.close();
		
		Session session2 = HibernateUtil.openSession();
		Query query2 = session2.createQuery("from BaseDict b where b.dictTypeCode='002'");
		query2.setCacheable(true);//设置使用查询缓存
		//从二级缓存查询
		List list2 = query2.list();
		System.out.println(list2);
		session2.close();
		
	}
	
	@Test
	public void test4(){
		Session session1 = HibernateUtil.openSession();
		//客户来源字典查询
		SQLQuery query1 = session1.createSQLQuery("select * from base_dict b where b.dict_type_code='002'");
		query1.addEntity(BaseDict.class);
		query1.setCacheable(true);//设置使用查询缓存
		List list = query1.list();
		System.out.println(list);
		session1.close();
		
		Session session2 = HibernateUtil.openSession();
		BaseDict baseDict = session2.get(BaseDict.class, "6");
		SQLQuery query2 = session2.createSQLQuery("select * from base_dict b where b.dict_type_code='002'");
		query2.addEntity(BaseDict.class);
		query2.setCacheable(true);//设置使用查询缓存
		List list2 = query2.list();
		System.out.println(list2);
		session2.close();
		
	}
	
	//原生sql
	@Test
	public void test3(){
		Session session1 = HibernateUtil.openSession();
		SQLQuery sqlQuery1 = session1.createSQLQuery("select count(*) count from cst_customer group by cust_level");
		//sqlQuery1.addEntity(CustomResult.class);
		sqlQuery1.addScalar("count", LongType.INSTANCE);
		sqlQuery1.setResultTransformer(Transformers.aliasToBean(CustomResult.class));
//		sqlQuery1.addEntity(CustomResult.class);
//		sqlQuery1.setCacheable(true);//设置使用查询缓存
//		sqlQuery1.setCacheRegion("sampleCache1");
		List list = sqlQuery1.list();
//		System.out.println(list);
		session1.close();
		
		Session session2 = HibernateUtil.openSession();
		SQLQuery sqlQuery2 = session2.createSQLQuery("select count(*) count from cst_customer group by cust_level");
		sqlQuery2.addEntity(CustomResult.class);
		sqlQuery2.setCacheable(true);//设置使用查询缓存
		sqlQuery2.setCacheRegion("sampleCache1");
		List list2 = sqlQuery2.list();
//		System.out.println(list2);
		session2.close();
		
	}
}
