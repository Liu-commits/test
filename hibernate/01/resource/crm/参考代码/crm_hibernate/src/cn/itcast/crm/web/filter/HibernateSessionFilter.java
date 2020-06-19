package cn.itcast.crm.web.filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.hibernate.Session;

import cn.itcast.crm.util.HibernateUtil;


public class HibernateSessionFilter implements Filter {
	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		chain.doFilter(request, response);
		Session session = HibernateUtil.getCurrentSession();
		session.close();
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

}

