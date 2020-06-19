package cn.itcast.hibernate.relation;

import java.util.Iterator;
import java.util.Set;

import org.apache.jasper.el.ELContextWrapper;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.Before;
import org.junit.Test;

import cn.itcast.crm.domain.CstCustomer;
import cn.itcast.crm.domain.CstLinkman;
import cn.itcast.crm.domain.SysRole;
import cn.itcast.crm.domain.SysUser;
import cn.itcast.crm.util.HibernateUtil;

/**
 * 
 * <p>Title: OneToMany</p>
 * <p>Description:多对多测试 </p>
 * <p>Company: www.itcast.com</p> 
 * @author	传智.燕青
 * @date	2016年2月17日
 * @version 1.0
 */
public class ManyToManyUserRole {

	
	//新增，父表和子表同时添加
	@Test
	public void insert1(){
		Session session = HibernateUtil.openSession();
		session.beginTransaction();
		//用户信息
		SysUser sysUser = new SysUser();
		sysUser.setUserCode("m0001");
		sysUser.setUserName("小明");
		sysUser.setUserPassword("123");
		sysUser.setUserState("1");
		
		//角色信息
		SysRole sysRole = new SysRole();
		sysRole.setRoleName("员工");
		
		//建立关系
		sysUser.getSysRoles().add(sysRole);
		sysRole.getSysUsers().add(sysUser);
		
		session.save(sysUser);
		session.save(sysRole);
		
		session.getTransaction().commit();
		session.close();
	}
	
	//新增，双方已添加，只维护关系，通过操作一方对象维护关系，注意在不维护关系的一方设置inverse="true"
	@Test
	public void insert2(){
		Session session = HibernateUtil.openSession();
		session.beginTransaction();
		//用户信息
		SysUser sysUser = session.get(SysUser.class, 4l);
		//角色信息
		SysRole sysRole = session.get(SysRole.class, 3l);
		sysUser.getSysRoles().add(sysRole);
		session.getTransaction().commit();
		session.close();
	}
	
	//删除，删除任意一方解除关系
	@Test
	public void delete1(){
		Session session = HibernateUtil.openSession();
		session.beginTransaction();
		//查询用户
		SysUser sysUser = session.get(SysUser.class, 4l);
		//删除用户
		session.delete(sysUser);
		session.getTransaction().commit();
		session.close();
	}
	
	//删除，双方不删除，只解除关系，解除一方的所有关系
	@Test
	public void delete2(){
		Session session = HibernateUtil.openSession();
		session.beginTransaction();
		//查询用户
		SysUser sysUser = session.get(SysUser.class, 6l);
		sysUser.getSysRoles().clear();
		session.getTransaction().commit();
		session.close();
	}
	
	//删除，双方不删除，只解除关系，解除一方的个别关系
	@Test
	public void delete3(){
		Session session = HibernateUtil.openSession();
		session.beginTransaction();
		//查询用户
		SysUser sysUser = session.get(SysUser.class, 8l);
		//某个角色
//		SysRole sysRole = new SysRole();
//		sysRole.setRoleId(1l);
		SysRole sysRole = session.get(SysRole.class, 2l);
		
		sysUser.getSysRoles().remove(sysRole);
		
		session.getTransaction().commit();
		session.close();
	}
	
}
