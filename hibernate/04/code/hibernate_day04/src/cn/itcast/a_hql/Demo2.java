package cn.itcast.a_hql;

import java.util.Arrays;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Test;

import cn.itcast.domain.Customer;
import cn.itcast.utils.HibernateUtils;

//学习HQL语法(不常用) - 多表查询语法
public class Demo2 {
	//回顾-原生SQL
	// 交叉连接-笛卡尔积(避免)
//		select * from A,B 
	// 内连接
//		|-隐式内连接
//			select * from A,B  where b.aid = a.id
//		|-显式内连接
//			select * from A inner join B on b.aid = a.id
	// 外连接
//		|- 左外
//			select * from A left [outer] join B on b.aid = a.id
//		|- 右外
//			select * from A right [outer] join B on b.aid = a.id
//---------------------------------------------------------------------
//HQL的多表查询
		//内连接(迫切)
		//外连接
//			|-左外(迫切)
//			|-右外(迫切)
	
	@Test
	//HQL 内连接 => 将连接的两端对象分别返回.放到数组中.
	public void fun1(){
		Session session = HibernateUtils.openSession();
		Transaction tx = session.beginTransaction();
		//----------------------------------------------------
		String hql = " from Customer c inner join c.linkMens ";
		
		Query query = session.createQuery(hql);
		
		List<Object[]> list = query.list();
		
		for(Object[] arr : list){
			System.out.println(Arrays.toString(arr));
		}
		//----------------------------------------------------
		tx.commit();
		session.close();
		
	}
	
	@Test
	//HQL 迫切内连接 => 帮我们进行封装.返回值就是一个对象
	public void fun2(){
		Session session = HibernateUtils.openSession();
		Transaction tx = session.beginTransaction();
		//----------------------------------------------------
		String hql = " from Customer c inner join fetch c.linkMens ";
		
		Query query = session.createQuery(hql);
		
		List<Customer> list = query.list();
		
		System.out.println(list);
		//----------------------------------------------------
		tx.commit();
		session.close();
		
	}
	
	@Test
	//HQL 左外连接 => 将连接的两端对象分别返回.放到数组中.
	public void fun3(){
		Session session = HibernateUtils.openSession();
		Transaction tx = session.beginTransaction();
		//----------------------------------------------------
		String hql = " from Customer c left join c.linkMens ";
		
		Query query = session.createQuery(hql);
		
		List<Object[]> list = query.list();
		
		for(Object[] arr : list){
			System.out.println(Arrays.toString(arr));
		}
		//----------------------------------------------------
		tx.commit();
		session.close();
		
	}
	@Test
	//HQL 右外连接 => 将连接的两端对象分别返回.放到数组中.
	public void fun4(){
		Session session = HibernateUtils.openSession();
		Transaction tx = session.beginTransaction();
		//----------------------------------------------------
		String hql = " from Customer c right join c.linkMens ";
		
		Query query = session.createQuery(hql);
		
		List<Object[]> list = query.list();
		
		for(Object[] arr : list){
			System.out.println(Arrays.toString(arr));
		}
		//----------------------------------------------------
		tx.commit();
		session.close();
		
	}
	
}
