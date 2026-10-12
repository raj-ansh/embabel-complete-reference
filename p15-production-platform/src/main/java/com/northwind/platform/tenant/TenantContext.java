package com.northwind.platform.tenant;

import com.northwind.platform.tenant.Tenants.Plan;

/**
 * Chapter 104: who a request is for. Built once per request from the verified token, then passed to the
 * agent as an input, to tools as tool call context, and to logs and metrics. Never built from request data.
 */
public record TenantContext(String tenantId, String tenantName, Plan plan, String userId, String voice) {
}
