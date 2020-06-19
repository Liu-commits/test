package cn.itcast.hibernate.query;

import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.Test;

import cn.itcast.crm.domain.CstCustomer;
import cn.itcast.crm.domain.CstLinkman;
import cn.itcast.crm.util.HibernateUtil;

/**
 * 
 * <p>Title: ObjNav</p>
 * <p>Description: 对象导航查询</p>
 * <p>Company: www.itcast.com</p> 
 * @author	传智.燕青
 * @date	2016年2月21日
 * @version 1.0
 */
public class ObjNav {
	
	//get方式对象导航
	@Test
	public void test1(){
		Session session = HibernateUtil.openSession();
		//查询客户
		CstCustomer cstCustomer = session.get(CstCustomer.class, 11l);
		//通过客户对象中的属性方式关联查询
		Set cstLinkmans = cstCustomer.getCstLinkmans();
		System.out.println(cstLinkmans);
	}

	//qbc方式对象导航
	@Test
	public void test2(){
		Session session = HibernateUtil.openSession();
		//查询客户
		Criteria criteria = session.createCriteria(CstCustomer.class);
		List<CstCustomer> list = criteria.list();
		for(CstCustomer customer:list){
			//获取客户下的联系人
			Set cstLinkmans = customer.getCstLinkmans();
			System.out.println(cstLinkmans);
		}
		
	}
	
	//hql方式对象导航
	@Test
	public void test3(){
		Session session = HibernateUtil.openSession();
		//查询客户
		Query query = session.createQuery("from CstCustomer");
		List<CstCustomer> list = query.list();
		for(CstCustomer customer:list){
			//获取客户下的联系人
			Set cstLinkmans = customer.getCstLinkmans();
			System.out.println(cstLinkmans);
		}
	}
	
	//qbc方式对象导航查询联系人
		@Test
		public void test4(){
			Session session = HibernateUtil.openSession();
			//查询客户
			Criteria criteria = session.createCriteria(CstLinkman.class);
			List<CstLinkman> list = criteria.list();
			for(CstLinkman linkman:list){
				//获取联系人所属客户
				CstCustomer cstCustomer = linkman.getCstCustomer();
				
				System.out.println(cstCustomer);
			}
			
		}
	
}
