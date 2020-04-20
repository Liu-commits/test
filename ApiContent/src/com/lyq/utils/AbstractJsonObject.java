package com.lyq.utils;

import java.util.Date;

public class AbstractJsonObject {
	private String code;
	// msg
	private String msg;

	private Long time = new Date().getTime();

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

	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
	}

	public void setContent(String code, String msg) {
		this.code = code;
		this.msg = msg;
	}

	public void setStatusObject(StatusObject statusObject) {
		this.code = statusObject.getCode();
		this.msg = statusObject.getMsg();
	}

}
