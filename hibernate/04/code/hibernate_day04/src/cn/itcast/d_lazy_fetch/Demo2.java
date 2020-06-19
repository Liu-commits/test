package cn.itcast.d_lazy_fetch;

import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.junit.Test;

import cn.itcast.domain.Customer;
import cn.itcast.domain.LinkMan;
import cn.itcast.utils.HibernateUtils;

//关联级别 延迟加载 & 抓取策略
public class Demo2 {
	
	@Test
	//fetch:select	单表查询
	//lazy:proxy  
		//customer-true 懒加载
	public void fun1(){
		Session session = HibernateUtils.openSession();
		Transaction tx = session.beginTransaction();
		//----------------------------------------------------
		
		LinkMan lm = session.get(LinkMan.class, 3l);
		
		Customer customer = lm.getCustomer();
		
		System.out.println(customer);
		
		//----------------------------------------------------
		tx.commit();
		session.close();
		
	}
	@Test
	//fetch:join	多表
	//lazy: 失效  
	public void fun3(){
		Session session = HibernateUtils.openSession();
		Transaction tx = session.beginTransaction();
		//----------------------------------------------------
		
		LinkMan lm = session.get(LinkMan.class, 3l);
		
		Customer customer = lm.getCustomer();
		
		System.out.println(customer);
		
		//----------------------------------------------------
		tx.commit();
		session.close();
		
	}
	@Test
	//fetch:select	单表查询
	//lazy:proxy  
		//customer-false 立即加载
	public void fun2(){
		Session session = HibernateUtils.openSession();
		Transaction tx = session.beginTransaction();
		//----------------------------------------------------
		
		LinkMan lm = session.get(LinkMan.class, 3l);
		
		Customer customer = lm.getCustomer();
		
		System.out.println(customer);
		
		//----------------------------------------------------
		tx.commit();
		session.close();
		
	}
}
