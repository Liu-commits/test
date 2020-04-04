package com.itheima.dao;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import com.itheima.domain.Product;
import com.itheima.utils.DataSourceUtils;

public class ProductDao {
	QueryRunner runner = new QueryRunner(DataSourceUtils.getDataSource());

	public List<Product> findAllProduct() throws SQLException {
		return runner.query("select * from product ", new BeanListHandler<Product>(Product.class));
		 
	}

	public int getTotalCount() throws SQLException {
		 Long count =  (Long) runner.query("select count(*) from product ", new ScalarHandler());
		 return count.intValue();
	}

	public List<Product> findProductListFotPageBean(int index,int currentCount) throws SQLException {
		
		String sql = "select * from product limit ?,?";
		
		List<Product> productList = runner.query(sql, new BeanListHandler<Product>(Product.class), index,currentCount);
		
		return productList;
	}

	public List<Product> finProductByWord(String word) throws SQLException {
		String sql = "select * from product where pname like ? limit 0,8";
		List<Product> query = runner.query(sql, new BeanListHandler<Product>(Product.class), "%"+word+"%");
		return query;
	}

}
