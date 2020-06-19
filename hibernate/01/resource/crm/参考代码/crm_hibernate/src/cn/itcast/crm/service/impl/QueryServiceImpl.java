package cn.itcast.crm.service.impl;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

import cn.itcast.crm.dao.CustomerDao;
import cn.itcast.crm.dao.CustomerDetailDao;
import cn.itcast.crm.dao.LinkmanDao;
import cn.itcast.crm.dao.SaleVisitDao;
import cn.itcast.crm.dao.impl.CustomerDaoImpl;
import cn.itcast.crm.dao.impl.CustomerDetailDaoImpl;
import cn.itcast.crm.dao.impl.LinkmanDaoImpl;
import cn.itcast.crm.dao.impl.SaleVisitDaoImpl;
import cn.itcast.crm.domain.CstCustomer;
import cn.itcast.crm.domain.CstCustomerDetail;
import cn.itcast.crm.domain.CstLinkman;
import cn.itcast.crm.domain.QueryVo;
import cn.itcast.crm.domain.SaleVisit;
import cn.itcast.crm.service.CustomerService;
import cn.itcast.crm.service.LinkmanService;
import cn.itcast.crm.service.QueryService;
import cn.itcast.crm.util.HibernateUtil;

public class QueryServiceImpl implements QueryService {

	
	
	//查询联系人表总记录数
	@Override
	public long findLinkmanCount(CstLinkman cstLinkman){
		LinkmanDao linkmanDao = new LinkmanDaoImpl();
		return linkmanDao.findLinkmanCount(cstLinkman);
	}

	@Override
	public List<CstLinkman> findLinkmanList(CstLinkman cstLinkman, int firstResult, int maxResults) {
		LinkmanDao linkmanDao = new LinkmanDaoImpl();
		return linkmanDao.findLinkmanList(cstLinkman, firstResult, maxResults);
		
	}
	

	//查询客户拜访总记录数
	@Override
	public long findSaleVisitCount(QueryVo queryVo){
		SaleVisitDao saleVisitDao = new SaleVisitDaoImpl();
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(SaleVisit.class);
		//拼接查询条件
		if(queryVo!=null){
			CstCustomer customer = queryVo.getCstCustomer();
			//拼接客户信息查询条件
			if(customer!=null){
				//查询关联 查询对象cstCustomer的别名，用于生成sql语句
				detachedCriteria.createAlias("cstCustomer", "cstCustomer");
				if (customer.getCustName() != null && !customer.getCustName().equals("")) {
					detachedCriteria.add(Restrictions.eq("cstCustomer.custName", customer.getCustName()));
				}
				//客户来源查询条件
				if(customer.getBaseDictBySource()!=null){
					String dictId = customer.getBaseDictBySource().getDictId();
					if(dictId!=null && !dictId.equals("")){
						detachedCriteria.add(Restrictions.eq("cstCustomer.baseDictBySource.dictId", dictId));
					}
				}
				
			}
		}
		return saleVisitDao.findSaleVisitCount(detachedCriteria);
	}

	@Override
	public List<SaleVisit> findSaleVisitList(QueryVo queryVo, int firstResult, int maxResults) {
		SaleVisitDao saleVisitDao = new SaleVisitDaoImpl();
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(SaleVisit.class);
		//拼接查询条件
		if(queryVo!=null){
			CstCustomer customer = queryVo.getCstCustomer();
			//拼接客户信息查询条件
			if(customer!=null){
				
				if (customer.getCustName() != null && !customer.getCustName().equals("")) {
					detachedCriteria.createAlias("cstCustomer", "cstCustomer");
					detachedCriteria.add(Restrictions.eq("cstCustomer.custName", customer.getCustName()));
				}
				//客户来源查询条件
				if(customer.getBaseDictBySource()!=null){
					String dictId = customer.getBaseDictBySource().getDictId();
					if(dictId!=null && !dictId.equals("")){
						detachedCriteria.add(Restrictions.eq("cstCustomer.baseDictBySource.dictId", dictId));
					}
				}
			}
		}
		return saleVisitDao.findSaleVisitList(detachedCriteria, firstResult, maxResults);
	}

}
