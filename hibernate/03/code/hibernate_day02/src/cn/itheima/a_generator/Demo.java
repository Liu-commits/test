package cn.itheima.a_generator;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.junit.Test;

import cn.itheima.domain.Customer;
import cn.itheima.utils.HibernateUtils;

//测试主键生成策略
public class Demo {

	@Test
	//保存客户
	public void fun1(){
		//1 获得session
		Session session = HibernateUtils.openSession();
		//2 控制事务
		Transaction tx = session.beginTransaction();
		//3执行操作
		Customer c = new Customer();
		c.setCust_name("联想");
		
		session.save(c);
		
		//4提交事务.关闭资源
		tx.commit();
		session.close();
		
		
	}
}
