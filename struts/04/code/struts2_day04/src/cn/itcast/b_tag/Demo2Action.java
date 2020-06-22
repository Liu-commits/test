package cn.itcast.b_tag;

import java.util.ArrayList;
import java.util.List;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;

public class Demo2Action extends ActionSupport {
	

	public String execute() throws Exception {
		
		List<String> list = new ArrayList<>();
		list.add("tom");
		list.add("jerry");
		list.add("jack");
		list.add("rose");
		list.add("hqy");
		
		ActionContext.getContext().put("list", list);
		return SUCCESS;
	}

	
	
}
