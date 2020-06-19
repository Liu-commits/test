package cn.itcast.b_criteria;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.junit.Test;

import cn.itcast.domain.Customer;
import cn.itcast.utils.HibernateUtils;

//学习Criteria语法
public class Demo {
	
	@Test
	//基本语法
	public void fun1(){
		Session session = HibernateUtils.openSession();
		Transaction tx = session.beginTransaction();
		//----------------------------------------------------
		
		Criteria c = session.createCriteria(Customer.class);
		
		List<Customer> list = c.list();
		
		System.out.println(list);
		
		//----------------------------------------------------
		tx.commit();
		session.close();
		
	}
	
	@Test
	//条件语法
	public void fun2(){
		Session session = HibernateUtils.openSession();
		Transaction tx = session.beginTransaction();
		//----------------------------------------------------
		
		Criteria c = session.createCriteria(Customer.class);
		
//		c.add(Restrictions.idEq(2l));
		c.add(Restrictions.eq("cust_id",2l));
		
		List<Customer> list = c.list();
		
		System.out.println(list);
		
		//----------------------------------------------------
		tx.commit();
		session.close();
		
	}
	
	@Test
	//分页语法 - 与HQL一样
	public void fun3(){
		Session session = HibernateUtils.openSession();
		Transaction tx = session.beginTransaction();
		//----------------------------------------------------
		
		Criteria c = session.createCriteria(Customer.class);
		//limit ?,? 
		c.setFirstResult(0);
		c.setMaxResults(2);
		
		List<Customer> list = c.list();
		
		System.out.println(list);
		
		//----------------------------------------------------
		tx.commit();
		session.close();
		
	}
	
	@Test
	//排序语法 
	public void fun4(){
		Session session = HibernateUtils.openSession();
		Transaction tx = session.beginTransaction();
		//----------------------------------------------------
		
		Criteria c = session.createCriteria(Customer.class);
		
		c.addOrder(Order.asc("cust_id"));
		//c.addOrder(Order.desc("cust_id"));
		
		List<Customer> list = c.list();
		
		System.out.println(list);
		
		//----------------------------------------------------
		tx.commit();
		session.close();
		
	}
	
	@Test
	//统计语法 
	public void fun5(){
		Session session = HibernateUtils.openSession();
		Transaction tx = session.beginTransaction();
		//----------------------------------------------------
		
		Criteria c = session.createCriteria(Customer.class);
		
		//设置查询目标
		c.setProjection(Projections.rowCount());
		
		List list = c.list();
		
		System.out.println(list);
		
		//----------------------------------------------------
		tx.commit();
		session.close();
		
	}
	
	
}
