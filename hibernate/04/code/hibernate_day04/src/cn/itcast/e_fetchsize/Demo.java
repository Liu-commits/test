package cn.itcast.e_fetchsize;

import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.junit.Test;

import cn.itcast.domain.Customer;
import cn.itcast.domain.LinkMan;
import cn.itcast.utils.HibernateUtils;

//抓取数量
public class Demo {
	
	@Test
	public void fun1(){
		Session session = HibernateUtils.openSession();
		Transaction tx = session.beginTransaction();
		//----------------------------------------------------
		
		String hql = "from Customer ";
		Query query = session.createQuery(hql);
		List<Customer> list = query.list();
		
		for(Customer c:list){
			System.out.println(c.getLinkMens());
		}
		
		//----------------------------------------------------
		tx.commit();
		session.close();
		
	}
	
	
}
