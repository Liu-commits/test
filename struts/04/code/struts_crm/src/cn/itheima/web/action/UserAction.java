package cn.itheima.web.action;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.ModelDriven;

import cn.itheima.domain.User;
import cn.itheima.service.UserService;
import cn.itheima.service.impl.UserServiceImpl;

public class UserAction extends ActionSupport implements ModelDriven<User> {
	private User user = new User();
	private UserService us  = new UserServiceImpl();
	
	
	
	public String login() throws Exception {
		//1 调用Service 执行登陆操作
		User u = us.login(user);
		//2 将返回的User对象放入session域作为登陆标识
		ActionContext.getContext().getSession().put("user", u);
		//3 重定向到项目的首页
		return "toHome";
	}










	@Override
	public User getModel() {
		return user;
	}

	
	
	
}
