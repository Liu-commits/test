package cn.itcast.crm.service.impl;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;

import cn.itcast.crm.dao.LinkmanDao;
import cn.itcast.crm.dao.SaleVisitDao;
import cn.itcast.crm.dao.impl.LinkmanDaoImpl;
import cn.itcast.crm.dao.impl.SaleVisitDaoImpl;
import cn.itcast.crm.domain.CstCustomer;
import cn.itcast.crm.domain.CstLinkman;
import cn.itcast.crm.domain.QueryVo;
import cn.itcast.crm.domain.SaleVisit;
import cn.itcast.crm.domain.SysUser;
import cn.itcast.crm.service.SaleService;
import cn.itcast.crm.util.HibernateUtil;

public class SaleServiceImpl implements SaleService {

	@Override
	public void insertSaleVisit(Long userId, Long custId, SaleVisit saleVisit) {
		//获取与线程绑定的session
		Session session = HibernateUtil.getCurrentSession();
		try {
			//开启事务
			session.beginTransaction();
			//客户
			CstCustomer cstCustomer = new CstCustomer();
			cstCustomer.setCustId(custId);
			saleVisit.setCstCustomer(cstCustomer);
			//用户
			SysUser sysUser = new SysUser();
			sysUser.setUserId(userId);
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
