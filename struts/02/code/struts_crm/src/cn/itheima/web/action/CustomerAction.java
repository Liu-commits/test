package cn.itheima.web.action;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.ModelDriven;

import cn.itheima.domain.Customer;
import cn.itheima.service.CustomerService;
import cn.itheima.service.impl.CustomerServiceImpl;

public class CustomerAction extends ActionSupport implements ModelDriven<Customer> {
	private CustomerService cs = new CustomerServiceImpl();
	private Customer customer = new Customer();
	
	public String list() throws Exception {
		//1 接受参数
		String cust_name = ServletActionContext.getRequest().getParameter("cust_name");
		//2 创建离线查询对象
		DetachedCriteria dc =DetachedCriteria.forClass(Customer.class);
		//3 判断参数拼装条件
		if(StringUtils.isNotBlank(cust_name)){
			dc.add(Restrictions.like("cust_name", "%"+cust_name+"%"));
		}
		//4 调用Service将离线对象传递
		List<Customer> list = cs.getAll(dc);
		//5 将返回的list放入request域.转发到list.jsp显示
		ServletActionContext.getRequest().setAttribute("list", list);
		return "list";
	}

	
	//添加客户
	public String add() throws Exception {
		//1 调用Service
		cs.save(customer);
		//2 重定向到列表action方法
		return "toList";
	}


	@Override
	public Customer getModel() {
		return customer;
	}

	
	
}
