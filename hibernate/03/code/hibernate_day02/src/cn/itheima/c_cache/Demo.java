package cn.itheima.c_cache;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.junit.Test;

import cn.itheima.domain.Customer;
import cn.itheima.utils.HibernateUtils;

//测试一级缓存
public class Demo {

	@Test
	//证明一级缓存存在
	public void fun1(){
		//1 获得session
		Session session = HibernateUtils.openSession();
		//2 控制事务
		Transaction tx = session.beginTransaction();
		//3执行操作
		
		Customer c1 = session.get(Customer.class, 1l);
		Customer c2 = session.get(Customer.class, 1l);
		Customer c3 = session.get(Customer.class, 1l);
		Customer c4 = session.get(Customer.class, 1l);
		Customer c5 = session.get(Customer.class, 1l);
		
		System.out.println(c3==c5);//true
		//4提交事务.关闭资源
		tx.commit();
		session.close();// 游离|托管 状态, 有id , 没有关联
		
		
	}
	
	@Test
	//
	public void fun2(){
		//1 获得session
		Session session = HibernateUtils.openSession();
		//2 控制事务
		Transaction tx = session.beginTransaction();
		//3执行操作
		
		Customer c1 = session.get(Customer.class, 1l);
		
		c1.setCust_name("哈哈");
		c1.setCust_name("传智播客");
		
		//4提交事务.关闭资源
		tx.commit();
		session.close();// 游离|托管 状态, 有id , 没有关联
		
		
	}
	
	@Test
	//持久化状态对象其实就是放入session缓存中的对象
	public void fun3(){
		//1 获得session
		Session session = HibernateUtils.openSession();
		//2 控制事务
		Transaction tx = session.beginTransaction();
		//3执行操作
		
		Customer c1 = new Customer();
		c1.setCust_id(1l);//托管|游离
		
		session.update(c1);//c1被放入session缓存了
		
		
		Customer c2 = session.get(Customer.class, 1l);
		
		//4提交事务.关闭资源
		tx.commit();
		session.close();// 游离|托管 状态, 有id , 没有关联
		
		
	}
	
}
