package com.itheima.service;

import java.sql.SQLException;
import java.util.List;

import com.itheima.dao.ProductDao;
import com.itheima.domain.Product;
import com.itheima.vo.PageBean;


public class ProductService {
	ProductDao dao = new  ProductDao();
	public List<Product> findAllProduct() throws SQLException {
		
		return dao.findAllProduct();
	}

	//分页操作
	public PageBean findPageBean(int currentPage,int currentCount) throws SQLException {
		// TODO Auto-generated method stub
		PageBean pageBean = new PageBean();
		//当前页
		pageBean.setCurrentPage(currentPage);
		//每页显示的条数
		pageBean.setCurrentCount(currentCount);
		//总条数
		int totalCount = dao.getTotalCount();
		pageBean.setTotalCount(totalCount);
		//总页数
		int totoalPage = (int) Math.ceil(1.0*totalCount/currentCount);
		pageBean.setTotalPage(totoalPage);
		//每页显示的数据 当前页数减一乘以每页显示的条数
		int index = (currentPage-1)*currentCount;
		List<Product> productList = dao.findProductListFotPageBean(index,currentCount);
		
		pageBean.setProductList(productList);
		return pageBean;
	}

	public List<Product> finProductByWord(String word) throws SQLException {
		return dao.finProductByWord(word);
		
	}
	

}
