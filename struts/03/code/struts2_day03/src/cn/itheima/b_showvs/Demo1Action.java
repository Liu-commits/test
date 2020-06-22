package cn.itheima.b_showvs;

import com.opensymphony.xwork2.ActionSupport;

public class Demo1Action extends ActionSupport {

	@Override
	public String execute() throws Exception {
		System.out.println("Demo1Action!!");
		return SUCCESS;
	}

	
}
