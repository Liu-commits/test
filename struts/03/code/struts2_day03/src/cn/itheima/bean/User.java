package cn.itheima.bean;

public class User {
	private String name;
	private Integer age;
	
	public User() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	
	public User(String name, Integer age) {
		super();
		this.name = name;
		this.age = age;
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


	@Override
	public String toString() {
		return "User [name=" + name + ", age=" + age + "]";
	}
	
	
}
