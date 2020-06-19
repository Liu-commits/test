package cn.itcast.hibernate.query;

import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.hql.internal.ast.tree.RestrictableStatement;
import org.junit.Test;

import cn.itcast.crm.domain.CstCustomer;
import cn.itcast.crm.util.HibernateUtil;

/**
 * 
 * <p>Title: InnerJoin</p>
 * <p>Description:内连接查询</p>
 * <p>Company: www.itcast.com</p> 
 * @author	传智.燕青
 * @date	2016年2月17日
 * @version 1.0
 */
public class Hql {

	//使用hql，一对多查询，查询客户信息关联查询联系人信息
	@Test
	public void onetomany(){
		Session session = HibernateUtil.openSession();
//		String hql = " from CstCustomer c inner join fetch c.cstLinkmans";
		String hql = " from CstLinkman c inner join fetch c.cstCustomer";
		Query query = session.createQuery(hql);
		List list = query.list();
		System.out.println(list.size());
		session.close();
		
		CstCustomer cstCustomer = (CstCustomer) list.get(0);
		Set cstLinkmans = cstCustomer.getCstLinkmans();
		System.out.println(cstLinkmans.size());
	}
	@Test
	public void manytomany(){
		Session session = HibernateUtil.openSession();
		String hql = " from SaleVisit s inner join fetch s.cstCustomer inner join fetch s.sysUser";
		Query query = session.createQuery(hql);
		List list = query.list();
		System.out.println(list.size());
		session.close();
		
	}
	//使用QBC，一对多查询，查询客户信息关联查询联系人信息
	@Test
	public void onetomany_qbc(){
		Session session = HibernateUtil.openSession();
		Criteria criteria = session.createCriteria(CstCustomer.class);
		//criteria.createCriteria("cstLinkmans").add(Restrictions.eq("lkmName", "王总"));
		List list = criteria.list();
		System.out.println(list.size());
		session.close();
		
		CstCustomer cstCustomer = (CstCustomer) list.get(0);
		Set cstLinkmans = cstCustomer.getCstLinkmans();
		System.out.println(cstLinkmans.size());
		
	}
}
