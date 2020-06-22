package cn.itcast.a_interceptor;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.MethodFilterInterceptor;

//继承:MethodFilterInterceptor 方法过滤拦截器
//功能: 定制拦截器拦截的方法.
//	定制哪些方法需要拦截.
//	定制哪些方法不需要拦截
public class MyInterceptor3 extends MethodFilterInterceptor{

	@Override
	protected String doIntercept(ActionInvocation invocation) throws Exception {
		//前处理
		System.out.println("MyInterceptor3 的前处理!");
		//放行
		String result = invocation.invoke();
		//后处理
		System.out.println("MyInterceptor3 的后处理!");
		
		return result;
	}

}
