package cn.itcast.crm.dao;

import java.util.List;

import org.hibernate.criterion.DetachedCriteria;

import cn.itcast.crm.domain.BaseDict;
import cn.itcast.crm.domain.CstLinkman;

public interface BaseDictDao {
	
	//数据字典列表查询
	public List<BaseDict> findBaseDictList(DetachedCriteria detachedCriteria);

}
