package com.lyq.utils;

public class StatusObject {
	// ״̬��
	private String code;

	// ״̬��Ϣ
	private String msg;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public StatusObject(String code, String msg) {
		super();
		this.code = code;
		this.msg = msg;
	}

}
