package cn.itcast.crm.service;

import java.util.List;

import cn.itcast.crm.domain.BaseDict;

public interface SystemService {

	//根据类型查询数据字典
	public List<BaseDict> findBaseDictListByType(String typecode);
}
