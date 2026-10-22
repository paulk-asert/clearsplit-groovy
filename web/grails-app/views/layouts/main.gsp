<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title><g:layoutTitle default="ClearSplit"/></title>
    <asset:link rel="icon" href="favicon.ico" type="image/x-ico"/>
    <asset:stylesheet src="application.css"/>
    <asset:javascript src="theme.js"/>
    <g:layoutHead/>
</head>
<body class="bg-body-tertiary">
<nav class="navbar bg-body border-bottom shadow-sm">
    <div class="container" style="max-width: 46rem">
        <a class="navbar-brand fw-semibold" href="${request.contextPath}/">ClearSplit</a>
        <span class="navbar-text small">verified expense splitting</span>
    </div>
</nav>
<main class="container py-4" style="max-width: 46rem">
    <g:layoutBody/>
</main>
<footer class="container pb-4 small text-body-secondary" style="max-width: 46rem">
    Splitting and balances proved at compile time with groovy-verify: shares add up exactly and balances always sum to zero.
</footer>
<asset:javascript src="application.js"/>
</body>
</html>
