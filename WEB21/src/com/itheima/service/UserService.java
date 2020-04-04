package com.itheima.service;

import java.sql.SQLException;

import com.itheima.dao.UserDao;

public class UserService {

	public boolean checkUsername(String username) throws SQLException {
		UserDao dao = new UserDao();
		Long isExit = dao.checkUsername(username);
		return isExit>0?true:false;
	}
	

}
