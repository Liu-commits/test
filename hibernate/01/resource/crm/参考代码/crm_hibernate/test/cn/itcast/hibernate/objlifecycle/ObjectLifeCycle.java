package cn.itcast.hibernate.objlifecycle;

import org.hibernate.Session;
import org.junit.Test;

import cn.itcast.crm.domain.CstCustomer;
import cn.itcast.crm.domain.CstCustomerDetail;
import cn.itcast.crm.util.HibernateUtil;

/**
 * 
 * <p>Title: ObjectState</p>
 * <p>Description: 对象的生命周期</p>
 * <p>Company: www.itcast.com</p> 
 * @author	传智.燕青
 * @date	2016年2月19日
 * @version 1.0
 */
public class ObjectLifeCycle {

	//对象的三种状态
	@Test
	public void lifeCycle(){
		
		Session session = HibernateUtil.openSession();
		//开启事务
		session.beginTransaction();
		//创建一个瞬时状态对象
		CstCustomer cstCustomer = new CstCustomer();
		cstCustomer.setCustName("张小明");
		//执行save，对象变为持久态
		session.save(cstCustomer);
		session.getTransaction().commit();
		
		//开启事务
		session.beginTransaction();
		//更新持久态对象中的内容
		cstCustomer.setCustName("张大明");
		//提交后持久对象的变更属性持久到数据库
		session.getTransaction().commit();

		//session关闭，对象变为托管态
		session.close();

		//新开一个session
		Session session2 = HibernateUtil.openSession();
		session2.beginTransaction();
		//更新托管态对象中的内容无法持久到数据库
		cstCustomer.setCustName("张小明");
		session2.getTransaction().commit();
		session2.close();
		
	}
	
	//测试saveOrUpdate方法
	@Test
	public void testSaveOrUpdate(){
		Session session = HibernateUtil.openSession();
		//开启事务
		session.beginTransaction();
		//创建一个瞬时状态对象
		CstCustomer cstCustomer = new CstCustomer();
		cstCustomer.setCustName("张小明");
		//对象为瞬时状态，执行saveOrUpdate发出insert语句 
		session.saveOrUpdate(cstCustomer);
		session.getTransaction().commit();
		session.close();
		//新开一个session
		session = HibernateUtil.openSession();
		//开启事务
		session.beginTransaction();
		cstCustomer.setCustName("张大明");
		//对象为托管状态，执行saveOrUpdate发出update语句 
		session.saveOrUpdate(cstCustomer);
		session.getTransaction().commit();
		session.close();
		
	}
	
	//一级缓存
	@Test
	public void testFirstCache_save(){
		Session session = HibernateUtil.openSession();
		//开启事务
		session.beginTransaction();
		//创建一个瞬时状态对象
		CstCustomer cstCustomer = new CstCustomer();
		cstCustomer.setCustName("张小明");
		//执行 save对象变为持久状态，并放入一级缓存
		session.save(cstCustomer);
		//新客户id
		Long custId = cstCustomer.getCustId();
		//从一级缓存中获取新对象，不向数据库发出sql
		CstCustomer cstCustomer2 = session.get(CstCustomer.class, custId);
		//提交事务，对象持久化到数据库
		session.getTransaction().commit();
		//session关闭一级缓存清空
		session.close();
		//再次开一个session
		Session session2 = HibernateUtil.openSession();
		//向数据库发出sql查询对象，并放入一级缓存
		cstCustomer2 = session.get(CstCustomer.class, custId);
		//不再发sql，直接从一级缓存取
		cstCustomer2 = session.get(CstCustomer.class, custId);
		//session关闭一级缓存清空
		session2.close();
				
	}
	
	//一级缓存-delete
	@Test
	public void testFirstCache_delete(){
		Session session = HibernateUtil.openSession();
		//开启事务
		session.beginTransaction();
		//查询对象，将持久对象放入一级缓存 
		CstCustomer cstCustomer = session.get(CstCustomer.class, 28l);
		CstCustomerDetail cstCustomerDetail = session.get(CstCustomerDetail.class, 28l);
		//执行删除，清空一级缓存
		session.delete(cstCustomerDetail);
		session.delete(cstCustomer);
		//新客户id
		Long custId = cstCustomer.getCustId();
		//从一级缓存中获取新对象，不向数据库发出sql
		CstCustomer cstCustomer2 = session.get(CstCustomer.class, custId);
		session.getTransaction().commit();
		//session关闭一级缓存清空
		session.close();
		
	}
	
	//一级缓存和副本测试
	@Test
	public void testFirstCache2() {
		Session session = HibernateUtil.openSession();
		//持久对象放入一级缓存
		CstCustomer cstCustomer = session.get(CstCustomer.class, 19l);
		//修改属性值，直接修改一级缓存的值，原始值保存在副本中
		cstCustomer.setCustName("北京黑马程序员");
		//在同一个session从一级缓存中获取"北京黑马程序员"
		CstCustomer cstCustomer2 = session.get(CstCustomer.class, 19l);

		// 开启事务
		session.beginTransaction();
		//执行提交，判断副本和一级缓存不一致则发出update语句
		session.getTransaction().commit();
		//关闭session，一级缓存清空
		session.close();

	}
	
}
