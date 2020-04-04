package com.itheima.dao;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.taglibs.standard.tag.common.sql.DataSourceUtil;

import com.itheima.utils.DataSourceUtils;

public class UserDao {
	QueryRunner runner = new QueryRunner(DataSourceUtils.getDataSource());
	public Long checkUsername(String username) throws SQLException {
		String sql = "select count(*) from user where username = ?";
		Long query =  (Long)runner.query(sql, new ScalarHandler(), username);
		return query;
	}

	
}
