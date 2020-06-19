package cn.itheima.service;

import java.util.List;

import cn.itheima.domain.Customer;

public interface CustomerService {
	//保存客户
	void save(Customer c);
	//获得所有客户
	List<Customer> getAll();

}
