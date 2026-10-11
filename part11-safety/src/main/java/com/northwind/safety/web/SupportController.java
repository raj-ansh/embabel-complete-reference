package com.northwind.safety.web;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.northwind.safety.agent.RefundApprovalAgent.ApprovalRequest;
import com.northwind.safety.agent.RefundApprovalAgent.ApprovedRefund;
import com.northwind.safety.security.CallerIdentity;
import com.northwind.safety.service.SupportAssistant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Chapter 84: identity comes from the verified token; the body carries only the question. */
@RestController
public class SupportController {

    public record Ask(@NotBlank @Size(max = 2000) String message) {
    }

    private final SupportAssistant assistant;
    private final AgentPlatform platform;

    public SupportController(SupportAssistant assistant, AgentPlatform platform) {
        this.assistant = assistant;
        this.platform = platform;
    }

    @PostMapping("/api/support/refund-request")
    public Map<String, String> refundRequest(@AuthenticationPrincipal Jwt token, @Valid @RequestBody Ask ask) {
        CallerIdentity caller = CallerIdentity.from(token);
        return Map.of("caller", caller.userId(), "answer", assistant.refundRequest(caller.userId(), ask.message()));
    }

    @PostMapping("/api/refunds/{orderNumber}/approve")
    public ApprovedRefund approve(@AuthenticationPrincipal Jwt token, @PathVariable String orderNumber,
                                  @RequestParam String amount) {
        return AgentInvocation.create(platform, ApprovedRefund.class)
                .withProcessOptions(CallerIdentity.from(token).processOptions())
                .invoke(new ApprovalRequest(orderNumber, amount));
    }
}
