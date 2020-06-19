package cn.itcast.c_lazy;

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

//懒加载|延迟加载
public class Demo {
	
	@Test
	// get方法 : 立即加载.执行方法时立即发送sql语句查询结果
	public void fun1(){
		Session session = HibernateUtils.openSession();
		Transaction tx = session.beginTransaction();
		//----------------------------------------------------
		
		Customer c = session.get(Customer.class, 2l);
		
		System.out.println(c);
		//----------------------------------------------------
		tx.commit();
		session.close();
		
	}
	
	@Test
	// load方法(默认):是在执行时,不发送任何sql语句.返回一个对象.使用该对象时,才执行查询.
	// 延迟加载: 仅仅获得没有使用.不会查询.在使用时才进行查询.
	// 是否对类进行延迟加载: 可以通过在class元素上配置lazy属性来控制.
		//lazy:true  加载时,不查询.使用时才查询b
		//lazy:false 加载时立即查询.
	public void fun2(){
		Session session = HibernateUtils.openSession();
		Transaction tx = session.beginTransaction();
		//----------------------------------------------------
		
		Customer c = session.load(Customer.class, 2l);
		
		//----------------------------------------------------
		tx.commit();
		session.close();
		System.out.println(c);
		
	}
	
}
