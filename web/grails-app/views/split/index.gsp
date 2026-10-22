<%@ page import="clearsplit.Group" %>
<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>ClearSplit</title>
</head>
<body>
<div class="d-flex align-items-baseline justify-content-between mb-3">
    <div class="text-body-secondary">${group.members().join(' / ')}</div>
    <g:form action="reset" method="post"><button class="btn btn-link btn-sm" type="submit">New group</button></g:form>
</div>

<ul class="nav nav-tabs mb-3">
    <g:each var="t" in="${['balances', 'expense', 'settle', 'history']}">
        <li class="nav-item">
            <g:link class="nav-link ${t == tab ? 'active' : ''}" action="index" params="[tab: t]">${t.capitalize()}</g:link>
        </li>
    </g:each>
</ul>

<g:if test="${flash.error}"><div class="alert alert-danger">${flash.error}</div></g:if>
<g:if test="${flash.message}"><div class="alert alert-success">${flash.message}</div></g:if>

<div class="card shadow-sm"><div class="card-body">
<g:if test="${tab == 'balances'}">
    <table class="table mb-2">
        <thead><tr><th>Member</th><th class="text-end">Balance</th><th class="text-end">Status</th></tr></thead>
        <tbody>
        <g:each var="name" in="${group.members()}" status="m">
            <g:set var="bal" value="${balances[m]}"/>
            <tr class="${bal > 0 ? 'table-success' : bal < 0 ? 'table-danger' : ''}">
                <td>${name}</td>
                <td class="text-end font-monospace">${Group.money(bal)}</td>
                <td class="text-end">${bal > 0 ? 'is owed' : bal < 0 ? 'owes' : 'settled'}</td>
            </tr>
        </g:each>
        </tbody>
        <tfoot>
        <tr><th>Total spent</th><th class="text-end font-monospace">${Group.money(group.totalSpent())}</th><th></th></tr>
        <tr><td class="text-body-secondary">Sum of balances</td><td class="text-end font-monospace text-body-secondary">${Group.money(balances.sum())}</td><td></td></tr>
        </tfoot>
    </table>
</g:if>

<g:if test="${tab == 'expense'}">
    <g:form action="expense" method="post">
        <div class="row g-3">
            <div class="col-12"><label class="form-label">What for</label>
                <input class="form-control" name="description" placeholder="Dinner"/></div>
            <div class="col-6"><label class="form-label">Paid by</label>
                <g:select class="form-select" name="payer" from="${group.members()}" keys="${0..<group.size()}"/></div>
            <div class="col-6"><label class="form-label">Amount</label>
                <input class="form-control" name="amount" type="number" step="0.01" min="0.01" placeholder="0.00" required/></div>
            <div class="col-12"><label class="form-label">Split equally among</label>
                <div>
                <g:each var="name" in="${group.members()}" status="m">
                    <div class="form-check form-check-inline">
                        <input class="form-check-input" type="checkbox" name="participant" value="${m}" id="p${m}" checked/>
                        <label class="form-check-label" for="p${m}">${name}</label>
                    </div>
                </g:each>
                </div></div>
            <div class="col-12"><button class="btn btn-primary" type="submit">Add expense</button></div>
        </div>
    </g:form>
</g:if>

<g:if test="${tab == 'settle'}">
    <g:form action="settle" method="post">
        <div class="row g-3">
            <div class="col-4"><label class="form-label">From</label>
                <g:select class="form-select" name="from" from="${group.members()}" keys="${0..<group.size()}"/></div>
            <div class="col-4"><label class="form-label">To</label>
                <g:select class="form-select" name="to" from="${group.members()}" keys="${0..<group.size()}" value="${1}"/></div>
            <div class="col-4"><label class="form-label">Amount</label>
                <input class="form-control" name="amount" type="number" step="0.01" min="0.01" placeholder="0.00" required/></div>
            <div class="col-12"><button class="btn btn-primary" type="submit">Record payment</button></div>
        </div>
    </g:form>
</g:if>

<g:if test="${tab == 'history'}">
    <g:if test="${!group.expenses() && !group.settlements()}"><p class="text-body-secondary mb-0">No transactions yet</p></g:if>
    <ul class="list-group list-group-flush">
        <g:each var="e" in="${group.expenses()}">
            <li class="list-group-item d-flex justify-content-between">
                <span><span class="badge text-bg-primary me-2">Expense</span>${e.description()}: ${group.members()[e.payer()]} paid,
                    <span class="text-body-secondary">split ${e.shares().count { it > 0 }} ways</span></span>
                <span class="font-monospace">${Group.money(e.amount())}</span>
            </li>
        </g:each>
        <g:each var="s" in="${group.settlements()}">
            <li class="list-group-item d-flex justify-content-between">
                <span><span class="badge text-bg-secondary me-2">Payment</span>${group.members()[s.from()]} paid ${group.members()[s.to()]}</span>
                <span class="font-monospace">${Group.money(s.amount())}</span>
            </li>
        </g:each>
    </ul>
</g:if>
</div></div>
</body>
</html>
