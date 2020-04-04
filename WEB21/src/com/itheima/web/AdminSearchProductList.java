package com.itheima.web;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanUtils;

import com.itheima.domain.Category;
import com.itheima.domain.Product;
import com.itheima.service.AdminProductService;
import com.itheima.vo.Condition;

public class AdminSearchProductList extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		// super.doGet(req, resp);
		req.setCharacterEncoding("utf-8");
		// 获得页面数据
		Map<String, String[]> map = req.getParameterMap();
		/*
		 * for(Entry<String, String[]> a:map.entrySet()){
		 * 
		 * System.out.println("键是"+a.getKey());
		 * 
		 * System.out.println("值是"+a.getValue()[0]);
		 * 
		 * }
		 */

		AdminProductService service = new AdminProductService();
		// 获得所有的商品的类别数据
		List<Category> categoryList = null;
		try {
			categoryList = service.findAllCategory();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		
		Condition condition = new Condition();
		List<Product> productList = null;
		try {
			BeanUtils.populate(condition, map);
			productList = service.findProductListByCondition(condition);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		req.setAttribute("condition", condition);
		req.setAttribute("categoryList", categoryList);
		req.setAttribute("productList", productList);
		req.getRequestDispatcher("/admin/product/list.jsp").forward(req, resp);
	}

}
