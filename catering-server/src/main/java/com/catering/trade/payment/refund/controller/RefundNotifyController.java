package com.catering.trade.payment.refund.controller;

import com.catering.shared.constant.MessageConstant;
import com.catering.trade.payment.refund.domain.RefundNotification;
import com.catering.trade.payment.infrastructure.RefundNotificationGateway;
import com.catering.trade.payment.refund.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信退款结果回调 HTTP 入口。
 */
@Slf4j
@RestController
@RequestMapping("/notify")
@RequiredArgsConstructor
public class RefundNotifyController {

    private final RefundNotificationGateway refundNotificationGateway;
    private final RefundService refundService;

    @PostMapping("/refundResult")
    public ResponseEntity<Map<String, String>> refundResult(HttpServletRequest request) {
        try {
            String body = StreamUtils.copyToString(
                    request.getInputStream(),
                    StandardCharsets.UTF_8
            );
            RefundNotification notification = refundNotificationGateway.verifyAndDecrypt(
                    request.getHeader("Wechatpay-Serial"),
                    request.getHeader("Wechatpay-Timestamp"),
                    request.getHeader("Wechatpay-Nonce"),
                    request.getHeader("Wechatpay-Signature"),
                    body
            );
            refundService.handleRefundResult(notification);
            log.info(
                    "退款回调处理成功，订单号：{}，退款单号：{}",
                    notification.getOrderNumber(),
                    notification.getRefundNo()
            );
            return ResponseEntity.ok(responseBody("SUCCESS", "SUCCESS"));
        } catch (Exception ex) {
            log.error("退款回调处理失败：{}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(responseBody("FAIL", MessageConstant.REFUND_NOTIFY_PROCESS_FAILED));
        }
    }

    private Map<String, String> responseBody(String code, String message) {
        Map<String, String> body = new HashMap<>();
        body.put("code", code);
        body.put("message", message);
        return body;
    }
}
