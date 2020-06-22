<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
   <%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
</head>
<body>
	<!-- struts2表单标签 -->
	<!-- 好处1: 内置了一套样式.  -->
	<!-- 好处2: 自动回显,根据栈中的属性  -->
	<!-- theme:指定表单的主题
			xhtml:默认
			simple:没有主题
	 -->
	<s:form action="Demo3Action" namespace="/" theme="xhtml" >
		<s:textfield name="name" label="用户名"  ></s:textfield>
		<s:password name="password" label="密码" ></s:password>
		<s:radio list="{'男','女'}" name="gender" label="性别" ></s:radio>
		<s:radio list="#{1:'男',0:'女'}" name="gender" label="性别" ></s:radio>
		<s:checkboxlist list="#{2:'抽烟',1:'喝酒',0:'烫头'}" name="habits" label="爱好" ></s:checkboxlist>
		<s:select list="#{2:'大专',1:'本科',0:'硕士'}" headerKey="" headerValue="---请选择---" name="edu" label="学历" >
		</s:select>
		<s:file name="photo" label="近照" ></s:file>
		<s:textarea name="desc" label="个人简介" ></s:textarea>
		<s:submit value="提交" ></s:submit>
	</s:form>
	
	<s:actionerror/>
</body>
</html>