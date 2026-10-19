package com.catering.trade.payment.infrastructure.mock;

import com.catering.properties.WeChatProperties;
import com.catering.trade.payment.config.PaymentProperties;
import com.catering.trade.payment.refund.domain.RefundNotification;
import com.catering.trade.payment.refund.domain.RefundRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 通过真实 HTTP 请求模拟微信退款结果通知。
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
public class MockWechatRefundCallbackClient {

    private final RestTemplate paymentMockRestTemplate;
    private final PaymentProperties paymentProperties;
    private final WeChatProperties weChatProperties;

    public void sendRefundSuccess(RefundRequest request) {
        RefundNotification notification = RefundNotification.builder()
                .eventId("MOCK_REFUND_EVENT_" + UUID.randomUUID().toString().replace("-", ""))
                .eventType("REFUND.SUCCESS")
                .mchId(weChatProperties.getMchid())
                .orderNumber(request.getOrderNumber())
                .transactionId(request.getTransactionId())
                .refundNo(request.getRefundNo())
                .refundId("MOCK_REFUND_ID_" + UUID.randomUUID().toString().replace("-", ""))
                .refundStatus("SUCCESS")
                .refundAmount(request.getRefundAmount().movePointRight(2).intValueExact())
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
                    paymentProperties.getMockRefundNotifyUrl(),
                    new HttpEntity<>(notification, headers),
                    Map.class
            );
            log.info(
                    "Mock 微信退款回调完成，订单号：{}，退款单号：{}，响应状态：{}",
                    request.getOrderNumber(),
                    request.getRefundNo(),
                    response.getStatusCodeValue()
            );
        } catch (Exception ex) {
            log.error(
                    "Mock 微信退款回调失败，订单号：{}，退款单号：{}",
                    request.getOrderNumber(),
                    request.getRefundNo(),
                    ex
            );
        }
    }
}
