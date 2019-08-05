<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Control Hotel Device</title>
</head>
<body>
	<h3 style="color: red;">Send Command</h3>

	<div id="command">
		<form:form action="http://localhost:8090/hotel/control"
			method="post" modelAttribute="emp">
			<p>
				<label>Enter Employee Id</label>
				 <input type="text" name="hotelId" value="7days" />
				 <input type="text" name="roomName" value="president1001" />
				 <input type="text" name="skillId" value="101" />
				 <input type="SUBMIT" value="Control" />
		</form:form>
	</div>
</body>
</html>
