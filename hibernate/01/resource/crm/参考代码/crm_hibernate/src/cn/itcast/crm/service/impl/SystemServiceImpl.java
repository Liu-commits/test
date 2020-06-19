package cn.itcast.crm.service.impl;

import java.util.List;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

import cn.itcast.crm.dao.BaseDictDao;
import cn.itcast.crm.dao.impl.BaseDictDaoImpl;
import cn.itcast.crm.domain.BaseDict;
import cn.itcast.crm.service.SystemService;

public class SystemServiceImpl implements SystemService{

	@Override
	public List<BaseDict> findBaseDictListByType(String typecode) {
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(BaseDict.class);
		detachedCriteria.add(Restrictions.eq("dictTypeCode", typecode));
		BaseDictDao baseDictDao = new BaseDictDaoImpl();
		
		return baseDictDao.findBaseDictList(detachedCriteria);
	}

}
