package cn.itcast.a_interceptor;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.Interceptor;

//拦截器:第一种创建方式
//拦截器生命周期:随项目的启动而创建,随项目关闭而销毁
public class MyInterceptor implements Interceptor {
	@Override
	//初始化方法
	public void init() {
		
	}

	@Override
	//拦截方法
	public String intercept(ActionInvocation arg0) throws Exception {
		return null;
	}

	
	@Override
	//销毁方法
	public void destroy() {
		
	}
}
