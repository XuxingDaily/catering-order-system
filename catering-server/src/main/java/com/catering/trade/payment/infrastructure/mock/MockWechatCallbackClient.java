package com.catering.trade.payment.infrastructure.mock;

import com.catering.properties.WeChatProperties;
import com.catering.trade.payment.config.PaymentProperties;
import com.catering.trade.payment.pay.domain.PaymentNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 本地微信支付模拟器，通过真实 HTTP 请求异步通知支付结果。
 */
@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "catering.payment",
        name = "provider",
        havingValue = "mock",
        matchIfMissing = true
)
@RequiredArgsConstructor
public class MockWechatCallbackClient {

    private final RestTemplate paymentMockRestTemplate;
    private final PaymentProperties paymentProperties;
    private final WeChatProperties weChatProperties;

    public void sendPaymentSuccess(
            String orderNumber,
            String transactionId,
            BigDecimal amount
    ) {
        PaymentNotification notification = PaymentNotification.builder()
                .eventId("MOCK_EVENT_" + UUID.randomUUID().toString().replace("-", ""))
                .eventType("TRANSACTION.SUCCESS")
                .appId(weChatProperties.getAppid())
                .mchId(weChatProperties.getMchid())
                .outTradeNo(orderNumber)
                .transactionId(transactionId)
                .tradeState("SUCCESS")
                .totalAmount(amount.movePointRight(2).intValueExact())
                .currency("CNY")
                .successTime(LocalDateTime.now().toString())
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Wechatpay-Serial", "MOCK_SERIAL");
        headers.set("Wechatpay-Timestamp", String.valueOf(Instant.now().getEpochSecond()));
        headers.set("Wechatpay-Nonce", "MOCK_NONCE");
        headers.set("Wechatpay-Signature", "MOCK_SIGNATURE");

        try {
            ResponseEntity<Map> response = paymentMockRestTemplate.postForEntity(
                    paymentProperties.getMockNotifyUrl(),
                    new HttpEntity<>(notification, headers),
                    Map.class
            );
            log.info(
                    "Mock 微信支付回调完成，订单号：{}，交易号：{}，响应状态：{}",
                    orderNumber,
                    transactionId,
                    response.getStatusCodeValue()
            );
        } catch (Exception ex) {
            // 异步线程不能把异常抛回已经结束的 payment() 请求，记录后便于本地排查。
            log.error("Mock 微信支付回调失败，订单号：{}，交易号：{}", orderNumber, transactionId, ex);
        }
    }
}
