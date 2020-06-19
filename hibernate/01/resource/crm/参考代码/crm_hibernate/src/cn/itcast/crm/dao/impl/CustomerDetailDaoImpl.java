package cn.itcast.crm.dao.impl;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import cn.itcast.crm.dao.CustomerDao;
import cn.itcast.crm.dao.CustomerDetailDao;
import cn.itcast.crm.domain.CstCustomer;
import cn.itcast.crm.domain.CstCustomerDetail;
import cn.itcast.crm.util.HibernateUtil;

public class CustomerDetailDaoImpl implements CustomerDetailDao {

	@Override
	public void insert(CstCustomerDetail cstCustomerDetail) {
		Session session = HibernateUtil.getCurrentSession();
		// 保存
		session.save(cstCustomerDetail);
	}
	// 删除客户详细信息
	public void delete(Long custId){
		Session session = HibernateUtil.getCurrentSession();
		CstCustomerDetail cstCustomerDetail = new CstCustomerDetail();
		cstCustomerDetail.setCustId(custId);
		session.delete(cstCustomerDetail);
	}

	// 更新客户详细信息
	public void update(CstCustomerDetail cstCustomerDetail){
		Session session = HibernateUtil.getCurrentSession();
		session.update(cstCustomerDetail);
	}
	//根据主键查询客户详细信息
	public CstCustomerDetail findCustomerDetailById(Long custId){
		Session session = HibernateUtil.openSession();
		return session.get(CstCustomerDetail.class, custId);
	}
	

}
