package cn.itheima.c_param;

import java.util.Date;

import com.opensymphony.xwork2.ActionSupport;

//struts2如何获得参数
//每次请求Action时都会创建新的Action实例对象
public class Demo8Action extends ActionSupport  {
	
	public Demo8Action() {
		super();
		System.out.println("demo8Action被创建了!");
	}


	//准备与参数键名称相同的属性
	private String name;
	//自动类型转换 只能转换8大基本数据类型以及对应包装类
	private Integer age;
	//支持特定类型字符串转换为Date ,例如 yyyy-MM-dd
	private Date   birthday;
	

	public String execute() throws Exception { 
		
		System.out.println("name参数值:"+name+",age参数值:"+age+",生日:"+birthday);
		
		return SUCCESS;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public Integer getAge() {
		return age;
	}


	public void setAge(Integer age) {
		this.age = age;
	}


	public Date getBirthday() {
		return birthday;
	}


	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}


	
}
