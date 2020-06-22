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
<!-- 遍历标签 iterator -->
<!-- ------------------------------------- -->
<s:iterator value="#list" >
	<s:property /><br>
</s:iterator>
<!-- ------------------------------------- --><hr>
<s:iterator value="#list" var="name" >
	<s:property value="#name" /><br>
</s:iterator>
<!-- ------------------------------------- --><hr>
<s:iterator begin="1" end="100" step="1"  >
	<s:property />|
</s:iterator>
<!-- ------------------if else elseif------------------- --><hr>

<s:if test="#list.size()==4">
	list长度为4!
</s:if>
<s:elseif test="#list.size()==3">
	list长度为3!
</s:elseif>
<s:else>
	list不3不4!
</s:else>

<!-- ------------------property 配合ognl表达式页面取值 ------------------- --><hr>

<s:property value="#list.size()" />
<s:property value="#session.user.name" />

</body>
</html>