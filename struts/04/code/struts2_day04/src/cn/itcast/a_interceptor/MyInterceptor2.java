package cn.itcast.a_interceptor;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

//创建方式2: 继承AbstractInterceptor -> struts2的体贴
//帮我们空实现了init 和 destory方法. 我们如果不需要实现这两个方法,就可以只实现intercept方法
public class MyInterceptor2 extends AbstractInterceptor {

	@Override
	public String intercept(ActionInvocation arg0) throws Exception {
		
		return null;
	}

}
