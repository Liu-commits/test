package com.lyq.status;

import com.lyq.utils.StatusObject;

public class StatusHouse {
	public static StatusObject COMMON_STATUS_OK = new StatusObject(
			StatusCode.CODE_SUCCESS, "���ʳɹ�");
	public static StatusObject COMMON_STATUS_ERROR = new StatusObject(
			StatusCode.CODE_ERROR, "���ʴ��󣬴����룺(" + StatusCode.CODE_ERROR + ")");
	public static StatusObject COMMON_STATUS_NO_LOGIN_OR_TIMEOUT = new StatusObject(
			StatusCode.CODE_ERROR_NO_LOGIN_OR_TIMEOUT, "δ��¼���¼��ʱ,�����µ�¼,�����룺("
					+ StatusCode.CODE_ERROR_NO_LOGIN_OR_TIMEOUT + ")");
	public static StatusObject COMMON_STATUS_ERROR_PROGRAM = new StatusObject(
			StatusCode.CODE_ERROR_PROGRAM, "�����쳣�������룺("
					+ StatusCode.CODE_ERROR_PROGRAM + ")");
	public static StatusObject COMMON_STATUS_ERROR_PARAMETER = new StatusObject(
			StatusCode.CODE_ERROR_PARAMETER, "�������󣬴����룺("
					+ StatusCode.CODE_ERROR_PARAMETER + ")");
	public static StatusObject COMMON_STATUS_EXIST_OPERATION = new StatusObject(
			StatusCode.CODE_ERROR_EXIST_OPERATION, "�Ѳ����������룺("
					+ StatusCode.CODE_ERROR_EXIST_OPERATION + ")");

}
