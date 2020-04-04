import static org.junit.Assert.*;

import java.sql.SQLException;

import org.junit.Test;

import com.itheima.dao.ProductDao;
import com.itheima.service.ProductService;
import com.itheima.vo.PageBean;
import com.itheima.web.CheckUsername;


public class Test01 {

	/*
	 * 单元测试
	 */
	@Test
	public void test() throws SQLException {
		int totalCount = new ProductDao().getTotalCount();
		System.out.println(totalCount);
	}
	
	@Test
	public void test01() throws SQLException {
		//一页 四条
		PageBean pageBean = new ProductService().findPageBean(1, 4);
		System.out.println(pageBean);
	}

	@Test
	
	public void test02() throws SQLException {
		//一页 四条
		new CheckUsername();
		
		
	}

}
