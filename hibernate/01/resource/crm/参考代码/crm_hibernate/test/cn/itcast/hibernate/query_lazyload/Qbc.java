package cn.itcast.hibernate.query_lazyload;

import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.junit.Test;

import cn.itcast.crm.domain.CstCustomer;
import cn.itcast.crm.domain.CstCustomerDetail;
import cn.itcast.crm.domain.CstLinkman;
import cn.itcast.crm.util.HibernateUtil;

/**
 * 
 * <p>Title: Lazyload</p>
 * <p>Description:延迟加载 </p>
 * <p>Company: www.itcast.com</p> 
 * @author	传智.燕青
 * @date	2016年2月19日
 * @version 1.0
 */
public class Qbc {

	//对象导航-一对多查询客户
	@Test
	public void test1(){
		
		Session session = HibernateUtil.openSession();
		Criteria criteria = session.createCriteria(CstCustomer.class);
		List list = criteria.list();
//		CstCustomer cstCustomer = (CstCustomer) list.get(0);
//		System.out.println(cstCustomer);
		session.close();
		
	}
	//对象导航-多对一查询联系人
	@Test
	public void test2(){
		Session session = HibernateUtil.openSession();
		Criteria criteria = session.createCriteria(CstLinkman.class);
		List list = criteria.list();
		CstLinkman cstLinkman = (CstLinkman) list.get(0);
		System.out.println(cstLinkman);
		session.close();
	}
	
	//对象导航-一对一查询客户详细信息
	@Test
	public void test3(){
		Session session = HibernateUtil.openSession();
		Criteria criteria = session.createCriteria(CstCustomer.class);
		List list = criteria.list();
		CstCustomer cstCustomer = (CstCustomer) list.get(0);
		CstCustomerDetail cstCustomerDetail = cstCustomer.getCstCustomerDetail();
		System.out.println(cstCustomerDetail);
		session.close();
	}
	
	
}
