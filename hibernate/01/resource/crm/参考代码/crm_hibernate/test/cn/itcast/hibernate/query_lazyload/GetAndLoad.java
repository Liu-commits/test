package cn.itcast.hibernate.query_lazyload;

import java.util.Set;

import org.hibernate.Session;
import org.junit.Test;

import cn.itcast.crm.domain.CstCustomer;
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
public class GetAndLoad {

	//load方法测试
	@Test
	public void test_get(){
		
		Session session = HibernateUtil.openSession();
		CstCustomer cstCustomer = session.get(CstCustomer.class, 25l);
		Set cstLinkmans = cstCustomer.getCstLinkmans();
		System.out.println(cstCustomer);
		
	}
	//load方法测试
	@Test
	public void test_load(){
		
		Session session = HibernateUtil.openSession();
		CstCustomer cstCustomer = session.load(CstCustomer.class, 11l);
		Set cstLinkmans = cstCustomer.getCstLinkmans();
		System.out.println(cstCustomer);
		
	}
	
	
}
