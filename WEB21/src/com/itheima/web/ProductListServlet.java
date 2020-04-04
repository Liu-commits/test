package com.itheima.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.itheima.domain.Product;
import com.itheima.service.ProductService;
import com.itheima.vo.PageBean;
import com.mchange.lang.IntegerUtils;

public class ProductListServlet extends HttpServlet {

	

	/**
	 * The doGet method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to get.
	 * 
	 * @param request the request send by the client to the server
	 * @param response the response send by the server to the client
	 * @throws ServletException if an error occurred
	 * @throws IOException if an error occurred
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		ProductService service = new ProductService();
		//当前页
		int currentPage ;
		try{
			int selectPage =Integer.parseInt( request.getParameter("currentPage"));
			currentPage = selectPage;
		}catch(Exception e){
			currentPage = 1;
			e.printStackTrace();
		}
		
//		if () {
//			
//		}
		
		//默认每页显示12条
		int currentCount = 12;
		PageBean<Product> pageBean = null;
		try {
			pageBean  = service.findPageBean(currentPage,currentCount);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		request.setAttribute("pageBean", pageBean);
		
		request.getRequestDispatcher("/product_list.jsp").forward(request, response);
		
		
		
	}

}
