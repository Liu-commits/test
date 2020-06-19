package cn.itcast.crm.domain;

/**
 * 
 * <p>Title: QueryVo</p>
 * <p>Description:查询对象，用于封装查询条件 </p>
 * <p>Company: www.itcast.com</p> 
 * @author	传智.燕青
 * @date	2016年2月25日
 * @version 1.0
 */
public class QueryVo {
	
	//客户信息
	private CstCustomer cstCustomer;
	//用户信息
	private SysUser sysUser;
	public CstCustomer getCstCustomer() {
		return cstCustomer;
	}
	public void setCstCustomer(CstCustomer cstCustomer) {
		this.cstCustomer = cstCustomer;
	}
	public SysUser getSysUser() {
		return sysUser;
	}
	public void setSysUser(SysUser sysUser) {
		this.sysUser = sysUser;
	}
	
	

}
