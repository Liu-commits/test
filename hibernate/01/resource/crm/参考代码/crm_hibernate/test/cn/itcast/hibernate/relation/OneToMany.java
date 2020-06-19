package cn.itcast.hibernate.relation;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.junit.Test;

import cn.itcast.crm.domain.CstCustomer;
import cn.itcast.crm.domain.CstLinkman;
import cn.itcast.crm.domain.SaleVisit;
import cn.itcast.crm.domain.SysUser;
import cn.itcast.crm.util.HibernateUtil;

/**
 * 
 * <p>Title: OneToMany</p>
 * <p>Description:一对多、多对一测试 </p>
 * <p>Company: www.itcast.com</p> 
 * @author	传智.燕青
 * @date	2016年2月17日
 * @version 1.0
 */
public class OneToMany {

	//新增，父表和子表同时添加
	@Test
	public void insert1(){
		Session session = HibernateUtil.openSession();
		session.beginTransaction();
		//客户信息
		CstCustomer cstCustomer = new CstCustomer();
		cstCustomer.setCustName("北京传智博客");
		
		//联系人信息
		CstLinkman cstLinkman = new CstLinkman();
		//联系人名称
		cstLinkman.setLkmName("王总");
		cstLinkman.setLkmPhone("66887788");
		
		//建立关系
		cstLinkman.setCstCustomer(cstCustomer);//设计关联的父表对象，直接插入外键值
		//执行保存
		session.save(cstCustomer);
		session.save(cstLinkman);
		
		System.out.println("新客户id="+cstCustomer.getCustId());
		//提交事务
		session.getTransaction().commit();
		session.close();
	}
	
	//新增，已有父表，通过父表对象添加子表
//	@Test
//	public void insert2(){
//		Session session = HibernateUtil.openSession();
//		session.beginTransaction();
//		//查询出父表记录
//		CstCustomer cstCustomer = session.get(CstCustomer.class, 11l);
////		CstCustomer cstCustomer = new CstCustomer();
////		cstCustomer.setCustId(8l);
//		//联系人信息
//		CstLinkman cstLinkman = new CstLinkman();
//		//联系人名称
//		cstLinkman.setLkmName("王总");
//		cstLinkman.setLkmPhone("66887788");
//		cstLinkman.setCstCustomer(cstCustomer);
////		session.save(cstLinkman);
//		cstCustomer.getCstLinkmans().add(cstLinkman);
//		
//		session.getTransaction().commit();
//		session.close();
//		
//	}
	
	//新增，已有父表，单独添加子表
	@Test
	public void insert3(){
		Session session = HibernateUtil.openSession();
		session.beginTransaction();
		//查询出父表记录
		CstCustomer cstCustomer = new CstCustomer();
		//客户id
		cstCustomer.setCustId(11l);
		//联系人信息
		CstLinkman cstLinkman = new CstLinkman();
		//联系人名称
		cstLinkman.setLkmName("王总");
		cstLinkman.setLkmPhone("66887788");
		cstLinkman.setCstCustomer(cstCustomer);
		session.save(cstLinkman);
		
		session.getTransaction().commit();
		session.close();
	}
	
	//删除 ，只删除子表
	@Test
	public void delete1(){
		Session session = HibernateUtil.openSession();
		session.beginTransaction();
		//删除子表记录
//		CstLinkman cstLinkman = session.get(CstLinkman.class, 3l);
		CstLinkman cstLinkman = new CstLinkman();
		cstLinkman.setLkmId(24l);
		session.delete(cstLinkman);
		session.getTransaction().commit();
		session.close();
	}

	//删除，删除 父表之前要手动将外键关联的子表先删除，再删除父表
	//注意，很多情况下由于业务数据关联较多、也可能为了以后的统计分析等会进行逻辑删除
	@Test
	public void delete2(){
		Session session = HibernateUtil.openSession();
		session.beginTransaction();
		//删除子表记录
		Query query = session.createQuery("delete CstLinkman where cstCustomer.custId = 8");
		query.executeUpdate();
		
		//删除父表记录
//		CstCustomer cstCustomer = session.get(CstCustomer.class, 7l);
		CstCustomer cstCustomer = new CstCustomer();
		cstCustomer.setCustId(8l);
		session.delete(cstCustomer);
		session.getTransaction().commit();
		session.close();
		
	}
	
	//删除，删除父表，级联删除子表
	//注意，如果由父表对象维护和子表的关系则会发出update 子表 set 外键=null的sql，解决方法在父表的hbm.xml中添加inverse="true"
	@Test
	public void delete3(){
		Session session = HibernateUtil.openSession();
		session.beginTransaction();
		CstCustomer cstCustomer = session.get(CstCustomer.class, 19l);
//		CstCustomer cstCustomer = new CstCustomer();
//		cstCustomer.setCustId(19l);
		session.delete(cstCustomer);
		session.getTransaction().commit();
		session.close();
	}
	
	//删除，通过父表对象删除子表
//	@Test
//	public void delete4(){
//		Session session = HibernateUtil.openSession();
//		session.beginTransaction();
//		CstCustomer cstCustomer = session.get(CstCustomer.class, 18l);
////			CstCustomer cstCustomer = new CstCustomer();
////			cstCustomer.setCustId(5l);
//		cstCustomer.getCstLinkmans().clear();
//
//		session.getTransaction().commit();
//		session.close();
//	}
	
	//更新，更新子表
	@Test
	public void update1(){
		Session session = HibernateUtil.openSession();
		session.beginTransaction();
		//查询子表记录
//		CstLinkman cstLinkman = session.get(CstLinkman.class, 6l);
		CstLinkman cstLinkman = new CstLinkman();
		cstLinkman.setLkmId(3l);
//		CstCustomer cstCustomer = session.get(CstCustomer.class, 4l);
		CstCustomer cstCustomer = new CstCustomer();
		cstCustomer.setCustId(9l);
		cstLinkman.setCstCustomer(cstCustomer);
		session.update(cstLinkman);
		session.getTransaction().commit();
		session.close();
	}
	//主键不能更新！！
	@Test
	public void update2(){
		Session session = HibernateUtil.openSession();
		session.beginTransaction();
		CstCustomer cstCustomer = session.get(CstCustomer.class, 6l);
		cstCustomer.setCustId(1l);
		session.update(cstCustomer);
		session.getTransaction().commit();
		session.close();
	}

	//添加客户拜访
	@Test
	public void insertSaleVisit(){
		
		//获取与线程绑定的session
		Session session = HibernateUtil.getCurrentSession();
		try {
			//开启事务
			session.beginTransaction();
			SaleVisit saleVisit = new SaleVisit();
			//客户
			CstCustomer cstCustomer = new CstCustomer();
			cstCustomer.setCustId(27l);
			saleVisit.setCstCustomer(cstCustomer);
			//用户
			SysUser sysUser = new SysUser();
			sysUser.setUserId(5l);
			saleVisit.setSysUser(sysUser);
			//添加客户拜访
			session.save(saleVisit);
			//提交事务
			session.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace();
			//回滚事务
			session.getTransaction().rollback();
		} finally {
			//关闭session
			HibernateUtil.closeSession();
		}
	}
	
}
