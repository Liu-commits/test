package cn.itcast.b_many2many;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Test;

import cn.itcast.domain.Role;
import cn.itcast.domain.User;
import cn.itcast.utils.HibernateUtils;

//多对多关系操作
public class Demo {
	@Test
	//保存员工以及角色
	public void fun1(){
		//1 获得session
		Session session = HibernateUtils.openSession();
		//2 开启事务
		Transaction tx = session.beginTransaction();
		//-------------------------------------------------
		//3操作
		//1> 创建两个 User
		User u1 = new User();
		u1.setUser_name("郝强勇");
		
		User u2 = new User();
		u2.setUser_name("金家德");
		
		//2> 创建两个 Role
		Role r1 = new Role();
		r1.setRole_name("保洁");
		
		Role r2 = new Role();
		r2.setRole_name("保安");
		//3> 用户表达关系
		u1.getRoles().add(r1);
		u1.getRoles().add(r2);
		
		u2.getRoles().add(r1);
		u2.getRoles().add(r2);
		
		//4> 角色表达关系
		r1.getUsers().add(u1);
		r1.getUsers().add(u2);
		
		r2.getUsers().add(u1);
		r2.getUsers().add(u2);
		
		//5> 调用Save方法一次保存
		session.save(u1);
		session.save(u2);
		session.save(r1);
		session.save(r2);
		//-------------------------------------------------
		//4提交事务
		tx.commit();
		//5关闭资源
		session.close();
	}
	
	
	@Test
	//为郝强勇新增一个角色
	public void fun3(){
		//1 获得session
		Session session = HibernateUtils.openSession();
		//2 开启事务
		Transaction tx = session.beginTransaction();
		//-------------------------------------------------
		//3操作
		//1> 获得郝强勇用户
		User user = session.get(User.class, 1l);
		//2> 创建公关角色
		Role r = new Role();
		r.setRole_name("男公关");
		//3> 将角色添加到用户中
		user.getRoles().add(r);
		//4> 将角色转换为持久化
		//session.save(r);
		//-------------------------------------------------
		//4提交事务
		tx.commit();
		//5关闭资源
		session.close();
	}
	
	@Test
	//为郝强勇解除一个角色
	public void fun4(){
		//1 获得session
		Session session = HibernateUtils.openSession();
		//2 开启事务
		Transaction tx = session.beginTransaction();
		//-------------------------------------------------
		//3操作
		//1> 获得郝强勇用户
		User user = session.get(User.class, 1l);
		//2> 获得要操作的角色对象(保洁,保安)
		Role r1 = session.get(Role.class, 1l);
		Role r2 = session.get(Role.class, 2l);
		//3> 将角色从用户的角色集合中移除
		user.getRoles().remove(r1);
		user.getRoles().remove(r2);
		
		//-------------------------------------------------
		//4提交事务
		tx.commit();
		//5关闭资源
		session.close();
	}
}
