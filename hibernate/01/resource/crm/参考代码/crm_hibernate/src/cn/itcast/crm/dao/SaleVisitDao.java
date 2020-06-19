package cn.itcast.crm.dao;

import java.util.List;

import org.hibernate.criterion.DetachedCriteria;

import cn.itcast.crm.domain.SaleVisit;

/**
 * 
 * <p>
 * Title: SaleVisitDao
 * </p>
 * <p>
 * Description:客户拜访dao
 * </p>
 * <p>
 * Company: www.itcast.com
 * </p>
 * 
 * @author 传智.燕青
 * @date 2016年2月21日
 * @version 1.0
 */
public interface SaleVisitDao {

	// 添加客户拜访
	public void insertSaleVisit(SaleVisit saleVisit);

	// 客户拜访列表总数
	public long findSaleVisitCount(DetachedCriteria detachedCriteria);
	// 客户拜访列表
	public List<SaleVisit> findSaleVisitList(DetachedCriteria detachedCriteria, int firstResult, int maxResults);

}
