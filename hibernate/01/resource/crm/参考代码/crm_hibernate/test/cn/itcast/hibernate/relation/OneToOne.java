package cn.itcast.hibernate.relation;

import org.hibernate.Session;
import org.junit.Test;

import cn.itcast.crm.domain.CstCustomer;
import cn.itcast.crm.domain.CstCustomerDetail;
import cn.itcast.crm.util.HibernateUtil;

/**
 * 
 * <p>
 * Title: OneToMany
 * </p>
 * <p>
 * Description:一对一
 * </p>
 * <p>
 * Company: www.itcast.com
 * </p>
 * 
 * @author 传智.燕青
 * @date 2016年2月17日
 * @version 1.0
 */
public class OneToOne {

	// 新增，父表和子表同时添加
	@Test
	public void insert1() {
		Session session = HibernateUtil.openSession();
		session.beginTransaction();
		// 客户信息
		CstCustomer cstCustomer = new CstCustomer();
		cstCustomer.setCustName("郑州传智");

		
		// 建立关系
//		cstCustomerDetail.setCstCustomer(cstCustomer);
		
		session.save(cstCustomer);
		
		// 客户详细信息
		CstCustomerDetail cstCustomerDetail = new CstCustomerDetail();
		cstCustomerDetail.setCustAddress("郑州高新区");
		//客户详细信息id和客户基本信息一致
		cstCustomerDetail.setCustId(cstCustomer.getCustId());
		session.save(cstCustomerDetail);

		session.getTransaction().commit();
		session.close();
	}

	// 新增，只添加子表
//	@Test
//	public void insert2() {
//		Session session = HibernateUtil.openSession();
//		// 客户信息
//		CstCustomer cstCustomer = session.get(CstCustomer.class, 35l);
//		CstCustomerDetail cstCustomerDetail = session.get(CstCustomerDetail.class, 35l);
//		if(cstCustomer != null && cstCustomerDetail ==null){
//			session.beginTransaction();
//			cstCustomerDetail = new CstCustomerDetail();
//			cstCustomerDetail.setCustAddress("郑州高新区");
//			cstCustomerDetail.setCstCustomer(cstCustomer);
//			session.save(cstCustomerDetail);
//			session.getTransaction().commit();
//		}
//		
//		session.close();
//	}

	// 删除，只删除子表
//	@Test
//	public void delete1() {
//		Session session = HibernateUtil.openSession();
//		session.beginTransaction();
//		// 客户详细信息
//		CstCustomerDetail cstCustomerDetail = session.get(CstCustomerDetail.class, 35l);
//		session.delete(cstCustomerDetail);
//		session.getTransaction().commit();
//		session.close();
//	}

	// 删除，删除父表，级联删除子表
//	@Test
//	public void delete2() {
//		Session session = HibernateUtil.openSession();
//		session.beginTransaction();
//		// 客户信息
//		CstCustomer cstCustomer = session.get(CstCustomer.class, 38l);
//		session.delete(cstCustomer);
//		session.getTransaction().commit();
//		session.close();
//	}

}
