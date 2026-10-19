package com.catering.trade.payment.infrastructure.mock;

import com.catering.trade.payment.config.PaymentProperties;
import com.catering.trade.payment.infrastructure.PaymentGateway;
import com.catering.vo.OrderPaymentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        prefix = "catering.payment",
        name = "provider",
        havingValue = "mock",
        matchIfMissing = true
)
@RequiredArgsConstructor
public class MockPaymentGateway implements PaymentGateway {

    private final TaskScheduler paymentMockTaskScheduler;
    private final MockWechatCallbackClient mockWechatCallbackClient;
    private final PaymentProperties paymentProperties;

    @Override
    public OrderPaymentVO createPayment(
            String orderNumber,
            BigDecimal amount,
            String description,
            String openid
    ) {
        String mockPrepayId = "mock_" + UUID.randomUUID().toString().replace("-", "");
        String mockTransactionId = "MOCK_TRANSACTION_"
                + UUID.randomUUID().toString().replace("-", "");

        // payment() 只负责创建预支付交易；模拟微信服务器稍后通过 HTTP 异步通知支付结果。
        paymentMockTaskScheduler.schedule(
                () -> mockWechatCallbackClient.sendPaymentSuccess(
                        orderNumber,
                        mockTransactionId,
                        amount
                ),
                Instant.now().plusMillis(paymentProperties.getMockCallbackDelayMillis())
        );

        return OrderPaymentVO.builder()
                .nonceStr(UUID.randomUUID().toString().replace("-", ""))
                .timeStamp(String.valueOf(System.currentTimeMillis() / 1000))
                .packageStr("prepay_id=" + mockPrepayId)
                .signType("RSA")
                .paySign("MOCK_PAY_SIGN_" + orderNumber)
                .build();
    }
}
