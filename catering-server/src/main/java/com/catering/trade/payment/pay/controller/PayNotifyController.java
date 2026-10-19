package com.catering.trade.payment.pay.controller;

import com.catering.shared.constant.MessageConstant;
import com.catering.trade.payment.pay.domain.PaymentNotification;
import com.catering.trade.payment.infrastructure.PaymentNotificationGateway;
import com.catering.trade.payment.pay.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StreamUtils;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信支付回调 HTTP 入口。
 */
@Slf4j
@RestController
@RequestMapping("/notify")
@RequiredArgsConstructor
public class PayNotifyController {

    private static final String RESPONSE_SUCCESS = "SUCCESS";
    private static final String RESPONSE_FAIL = "FAIL";

    private final PaymentNotificationGateway paymentNotificationGateway;
    private final PaymentService paymentService;

    /**
     * 接收微信支付成功通知。
     *
     * <p>Controller 只负责读取 HTTP 协议数据并返回微信要求的响应；验签解密和
     * 支付业务分别委托给网关与支付服务。</p>
    */
    @PostMapping("/paySuccess")
    public ResponseEntity<Map<String, String>> paySuccessNotify(HttpServletRequest request) {
        try {
            // 直接读取原始报文，避免消息转换器改写 JSON；真实微信验签也依赖原始字节内容。
            String body = StreamUtils.copyToString(
                    request.getInputStream(),
                    StandardCharsets.UTF_8
            );
            PaymentNotification notification = paymentNotificationGateway.verifyAndDecrypt(
                    request.getHeader("Wechatpay-Serial"),
                    request.getHeader("Wechatpay-Timestamp"),
                    request.getHeader("Wechatpay-Nonce"),
                    request.getHeader("Wechatpay-Signature"),
                    body
            );
            paymentService.handlePaymentSuccess(notification);
            log.info(
                    "微信支付回调处理成功，商户订单号：{}，微信支付交易号：{}",
                    notification.getOutTradeNo(),
                    notification.getTransactionId()
            );
            return ResponseEntity.ok(responseBody(RESPONSE_SUCCESS, RESPONSE_SUCCESS));
        } catch (Exception ex) {
            // 返回非 2xx，微信支付平台会按其重试策略再次发送通知。
            log.error("微信支付回调处理失败：{}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(responseBody(RESPONSE_FAIL, MessageConstant.PAYMENT_NOTIFY_PROCESS_FAILED));
        }
    }

    private Map<String, String> responseBody(String code, String message) {
        Map<String, String> body = new HashMap<>();
        body.put("code", code);
        body.put("message", message);
        return body;
    }
}
