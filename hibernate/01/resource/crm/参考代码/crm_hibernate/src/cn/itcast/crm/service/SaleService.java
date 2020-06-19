package cn.itcast.crm.service;

import java.util.List;

import cn.itcast.crm.domain.CstLinkman;
import cn.itcast.crm.domain.QueryVo;
import cn.itcast.crm.domain.SaleVisit;

public interface SaleService {

	//添加客户拜访
	public void insertSaleVisit(Long userId,Long custId,SaleVisit saleVisit);
	
}
