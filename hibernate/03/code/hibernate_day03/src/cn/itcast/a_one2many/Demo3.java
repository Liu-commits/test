package cn.itcast.a_one2many;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Test;

import cn.itcast.domain.Customer;
import cn.itcast.domain.LinkMan;
import cn.itcast.utils.HibernateUtils;

//操作进阶--关系维护属性
public class Demo3 {
	@Test
	//保存客户 以及客户 下的联系人
	public void fun1(){
		//1 获得session
		Session session = HibernateUtils.openSession();
		//2 开启事务
		Transaction tx = session.beginTransaction();
		//-------------------------------------------------
		//3操作
		Customer c = new Customer();
		c.setCust_name("传智播客");
		
		LinkMan lm1 = new LinkMan();
		lm1.setLkm_name("黎活明");
		
		LinkMan lm2 = new LinkMan();
		lm2.setLkm_name("刘悦东");
		
		//表达一对多,客户下有多个联系人. 
		// 如果客户放弃维护与联系人的关系. 维护关系的代码可以省略
		//c.getLinkMens().add(lm1);
		//c.getLinkMens().add(lm2);
		
		//表达对对对,联系人属于哪个客户
		lm1.setCustomer(c);
		lm2.setCustomer(c);
		
		
		session.save(c);
		session.save(lm1);
		session.save(lm2);
		
		//-------------------------------------------------
		//4提交事务
		tx.commit();
		//5关闭资源
		session.close();
	}
	
	@Test
	//删除客户
	public void fun2(){
		//1 获得session
		Session session = HibernateUtils.openSession();
		//2 开启事务
		Transaction tx = session.beginTransaction();
		//-------------------------------------------------
		//3操作
		Customer customer = session.get(Customer.class, 1l);
		
		session.delete(customer);
		//-------------------------------------------------
		//4提交事务
		tx.commit();
		//5关闭资源
		session.close();
	}
		
}
