package cn.itheima.a_ognl;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import cn.itheima.bean.User;
import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlException;

//展示OGNL语法
public class Demo {
	@Test
	//准备工作
	public void fun1() throws Exception{
		//准备ONGLContext
			//准备Root
			User rootUser = new User("tom",18);
			//准备Context
			Map<String,User> context = new HashMap<String,User>();
			context.put("user1", new User("jack",18));
			context.put("user2", new User("rose",22));
		OgnlContext oc = new OgnlContext();
		//将rootUser作为root部分
		oc.setRoot(rootUser);
		//将context这个Map作为Context部分
		oc.setValues(context);
		//书写OGNL
		Ognl.getValue("", oc, oc.getRoot());
	}
	
	@Test
	//基本语法演示
	//取出root中的属性值
	public void fun2() throws Exception{
		//准备ONGLContext
			//准备Root
			User rootUser = new User("tom",18);
			//准备Context
			Map<String,User> context = new HashMap<String,User>();
			context.put("user1", new User("jack",18));
			context.put("user2", new User("rose",22));
		OgnlContext oc = new OgnlContext();
		oc.setRoot(rootUser);
		oc.setValues(context);
		//书写OGNL
		
		//取出root中user对象的name属性
		String name = (String) Ognl.getValue("name", oc, oc.getRoot());
		Integer age = (Integer) Ognl.getValue("age", oc, oc.getRoot());
		System.out.println(name);
		System.out.println(age);
	}
	@Test
	//基本语法演示
	//取出context中的属性值
	public void fun3() throws Exception{
		//准备ONGLContext
			//准备Root
			User rootUser = new User("tom",18);
			//准备Context
			Map<String,User> context = new HashMap<String,User>();
			context.put("user1", new User("jack",18));
			context.put("user2", new User("rose",22));
		OgnlContext oc = new OgnlContext();
		oc.setRoot(rootUser);
		oc.setValues(context);
		//书写OGNL
		
		//取出context中键为user1对象的name属性
		String name = (String) Ognl.getValue("#user1.name", oc, oc.getRoot());
		String name2 = (String) Ognl.getValue("#user2.name", oc, oc.getRoot());
		Integer age = (Integer) Ognl.getValue("#user2.age", oc, oc.getRoot());
		System.out.println(name);
		System.out.println(name2);
		System.out.println(age);
	}
	
	@Test
	//基本语法演示
	//为属性赋值
	public void fun4() throws Exception{
		//准备ONGLContext
			//准备Root
			User rootUser = new User("tom",18);
			//准备Context
			Map<String,User> context = new HashMap<String,User>();
			context.put("user1", new User("jack",18));
			context.put("user2", new User("rose",22));
		OgnlContext oc = new OgnlContext();
		oc.setRoot(rootUser);
		oc.setValues(context);
		//书写OGNL
		
		//将root中的user对象的name属性赋值
		Ognl.getValue("name='jerry'", oc, oc.getRoot());
		String name = (String) Ognl.getValue("name", oc, oc.getRoot());
		
		String name2 = (String) Ognl.getValue("#user1.name='郝强勇',#user1.name", oc, oc.getRoot());
		System.out.println(name);
		System.out.println(name2);
	}
	
	@Test
	//基本语法演示
	//调用方法
	public void fun5() throws Exception{
		//准备ONGLContext
			//准备Root
			User rootUser = new User("tom",18);
			//准备Context
			Map<String,User> context = new HashMap<String,User>();
			context.put("user1", new User("jack",18));
			context.put("user2", new User("rose",22));
		OgnlContext oc = new OgnlContext();
		oc.setRoot(rootUser);
		oc.setValues(context);
		//书写OGNL
		
		//调用root中user对象的setName方法
		Ognl.getValue("setName('lilei')", oc, oc.getRoot());
		String name = (String) Ognl.getValue("getName()", oc, oc.getRoot());
		
		String name2 = (String) Ognl.getValue("#user1.setName('lucy'),#user1.getName()", oc, oc.getRoot());
		
		
		System.out.println(name);
		System.out.println(name2);
	}
	
	
	@Test
	//基本语法演示
	//调用静态方法
	public void fun6() throws Exception{
		//准备ONGLContext
			//准备Root
			User rootUser = new User("tom",18);
			//准备Context
			Map<String,User> context = new HashMap<String,User>();
			context.put("user1", new User("jack",18));
			context.put("user2", new User("rose",22));
		OgnlContext oc = new OgnlContext();
		oc.setRoot(rootUser);
		oc.setValues(context);
		//书写OGNL
		
		String name = (String) Ognl.getValue("@cn.itheima.a_ognl.HahaUtils@echo('hello 强勇!')", oc, oc.getRoot());
		//Double pi = (Double) Ognl.getValue("@java.lang.Math@PI", oc, oc.getRoot());
		Double pi = (Double) Ognl.getValue("@@PI", oc, oc.getRoot());
		System.out.println(name);
		System.out.println(pi);
	}	
	
	@Test
	//基本语法演示
	//ognl创建对象-list|map
	public void fun7() throws Exception{
		//准备ONGLContext
			//准备Root
			User rootUser = new User("tom",18);
			//准备Context
			Map<String,User> context = new HashMap<String,User>();
			context.put("user1", new User("jack",18));
			context.put("user2", new User("rose",22));
		OgnlContext oc = new OgnlContext();
		oc.setRoot(rootUser);
		oc.setValues(context);
		//书写OGNL
		
		//创建list对象
		Integer size = (Integer) Ognl.getValue("{'tom','jerry','jack','rose'}.size()", oc, oc.getRoot());
		String name = (String) Ognl.getValue("{'tom','jerry','jack','rose'}[0]", oc, oc.getRoot());
		String name2 = (String) Ognl.getValue("{'tom','jerry','jack','rose'}.get(1)", oc, oc.getRoot());
	
		/*System.out.println(size);
		System.out.println(name);
		System.out.println(name2);*/
		//创建Map对象
		Integer size2 = (Integer) Ognl.getValue("#{'name':'tom','age':18}.size()", oc, oc.getRoot());
		String name3  = (String) Ognl.getValue("#{'name':'tom','age':18}['name']", oc, oc.getRoot());
		Integer age  = (Integer) Ognl.getValue("#{'name':'tom','age':18}.get('age')", oc, oc.getRoot());
		System.out.println(size2);
		System.out.println(name3);
		System.out.println(age);
	}	

}
