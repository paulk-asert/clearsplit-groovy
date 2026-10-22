<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>ClearSplit</title>
</head>
<body>
<div class="card shadow-sm">
    <div class="card-body">
        <h1 class="h4">Start a group</h1>
        <p class="text-body-secondary">Who is sharing expenses? One name per line, or separated by commas.</p>
        <g:if test="${flash.error}"><div class="alert alert-danger">${flash.error}</div></g:if>
        <g:form action="start" method="post">
            <textarea class="form-control mb-3" name="members" rows="4" placeholder="Alice&#10;Bob&#10;Carol" autofocus></textarea>
            <button class="btn btn-primary w-100" type="submit">Start</button>
        </g:form>
    </div>
</div>
</body>
</html>
