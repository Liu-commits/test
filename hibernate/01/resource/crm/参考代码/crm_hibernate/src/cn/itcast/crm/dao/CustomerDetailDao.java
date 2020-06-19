package cn.itcast.crm.dao;

import java.util.List;

import org.hibernate.Session;

import cn.itcast.crm.domain.CstCustomer;
import cn.itcast.crm.domain.CstCustomerDetail;
import cn.itcast.crm.util.HibernateUtil;

public interface CustomerDetailDao {

	// 添加客户详细信息
	public void insert(CstCustomerDetail customerDetail);
	// 删除客户
	public void delete(Long custId);

	// 更新客户
	public void update(CstCustomerDetail cstCustomerDetail);
	//查询客户详细信息
	public CstCustomerDetail findCustomerDetailById(Long custId);

	
}
