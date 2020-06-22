package cn.itheima.c_param;

import com.opensymphony.xwork2.ActionSupport;

import cn.itheima.domain.User;

//struts2如何获得参数-方式2
public class Demo9Action extends ActionSupport  {
	//准备user对象
	private User user;

	public String execute() throws Exception { 
		
		System.out.println(user);
		
		return SUCCESS;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	
}
