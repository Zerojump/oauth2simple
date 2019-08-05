<%@taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Apply for Token</title>
</head>
<body>
	<h3 style="color: red;">Apply for Token</h3>

	<div id="apply4Token">
		<form:form action="http://localhost:8011/oauth/authorize"
			method="post" modelAttribute="emp">
			<p>
				 <input type="text" name="response_type" value="code" />
				 <input type="text" name="client_id" value="hotel-client" />
				 <input type="text" name="redirect_uri" value="http://localhost:8090/hotel/apply4Token" />
				 <input type="text" name="scope" value="read" /> 
				 <input type="SUBMIT" value="Apply" />
		</form:form>
	</div>
</body>
</html>
