package cn.itcast.hibernate.first;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.junit.Before;
import org.junit.Test;

import cn.itcast.crm.domain.CstCustomer;
import cn.itcast.crm.util.HibernateUtil;

public class CrudTest {

	private SessionFactory sessionFactory;

	@Before
	public void setUp() {

		//默认加载classpath下hibernate.cfg.xml
		sessionFactory = new Configuration().configure().buildSessionFactory();
		//也可以指定要加载的hibernate.cfg.xml文件位置（基于classpath）
//		sessionFactory = new Configuration().configure("hibernate.cfg.xml").buildSessionFactory();
//		final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
//				.configure("hibernate.cfg.xml") // configures settings from hibernate.cfg.xml
//				.build();
//		try {
//			sessionFactory = new MetadataSources( registry ).buildMetadata().buildSessionFactory();
//		}
//		catch (Exception e) {
//			// The registry would be destroyed by the SessionFactory, but we had trouble building the SessionFactory
//			// so destroy it manually.
//			StandardServiceRegistryBuilder.destroy( registry );
//		}
	}

	//添加记录
	@Test
	public void testInsert() {
		//创建会话
		Session session = sessionFactory.openSession();
		//开启事务
		Transaction transaction = session.beginTransaction();
		try {
			//定义pojo对象
			CstCustomer cstCustomer = new CstCustomer();
			//设置属性值
			cstCustomer.setCustName("北京黑马程序员");
//			cstCustomer.setCustLevel("大客户");
			//执行保存操作
			session.save(cstCustomer);
			//提交事务，此时将客户信息持久化到数据库
			transaction.commit();
		} catch (Exception e) {
			e.printStackTrace();
			//事务回滚
			transaction.rollback();
			//上边的代码或者写为
			//session.getTransaction().rollback();
		}finally{
			//关闭session
			session.close();
		}		
	}
	
	//根据主键查询记录
	@Test
	public void testQueryById(){
		//创建会话
		Session session = sessionFactory.openSession();
		CstCustomer cstCustomer = (CstCustomer) session.get(CstCustomer.class, 27l);
		System.out.println(cstCustomer);
		//关闭session
		session.close();
	}
	
	//根据主键查询记录
	@Test
	public void testGetById() {

//		Session session = sessionFactory.openSession();
		//第一次调用getCurrentSession创建一个新session并与当前线程绑定
		Session session = HibernateUtil.getCurrentSession();
		//第二次调用getCurrentSession获取与线程绑定的session，和上边的session是同一个对象
		Session session2 = HibernateUtil.getCurrentSession();
		//参考：1、pojo类型，2、主键值（主键值根据实际需要修改）
		CstCustomer cstCustomer = (CstCustomer) session.get(CstCustomer.class, "40287181529d45d201529d45d4b20000");
		System.out.println(cstCustomer);
//		session.close();
		HibernateUtil.closeSession();
	}
	//删除记录
	@Test
	public void testDelete() {
		Session session = sessionFactory.openSession();
		try {
			//开启事务
			Transaction transaction = session.beginTransaction();
			CstCustomer cstCustomer = new CstCustomer();
			//设置要删除对象的主键（主键值根据实际需要修改）
			cstCustomer.setCustId(80l);
			//执行删除操作
			session.delete(cstCustomer);
			//提交事务
			transaction.commit();
		} catch (Exception e) {
			e.printStackTrace();
			//回滚事务
			session.getTransaction().rollback();
		}finally{
			session.close();
		}

	}
	//更新记录
	@Test
	public void testUpdate() {
		Session session = sessionFactory.openSession();
		
		try {
			//开启事务
			Transaction transaction = session.beginTransaction();
			Long custId = 27l;
			//查询出对象
			CstCustomer cstCustomer = (CstCustomer) session.get(CstCustomer.class, custId);
			//设置要更新的属性值
			cstCustomer.setCustName("北京传智播客");
			//执行更新操作
			session.update(cstCustomer);
			//提交事务
			transaction.commit();
		} catch (Exception e) {
			e.printStackTrace();
			//回滚事务
			session.getTransaction().rollback();
		}finally{
			session.close();
		}
		
	}
	
	//自定义条件查询-查询全部，使用HQL
	@Test
	public void testQueryByHQL(){
		Session session = sessionFactory.openSession();
		//Customer为类名，主键大小写和类名保持一致
		String hql = "from CstCustomer";
		//获取查询对象
		Query query = session.createQuery(hql);
		List list = query.list();
		System.out.println(list);
		session.close();
	}



}
