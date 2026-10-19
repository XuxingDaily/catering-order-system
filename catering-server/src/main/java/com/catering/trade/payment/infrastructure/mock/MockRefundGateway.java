package com.catering.trade.payment.infrastructure.mock;

import com.catering.trade.payment.config.PaymentProperties;
import com.catering.trade.payment.refund.domain.RefundRequest;
import com.catering.trade.payment.infrastructure.RefundGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 本地退款平台模拟器，受理后异步发送退款成功通知。
 */
@Component
@ConditionalOnProperty(
        prefix = "catering.payment",
        name = "provider",
        havingValue = "mock",
        matchIfMissing = true
)
@RequiredArgsConstructor
public class MockRefundGateway implements RefundGateway {

    private final TaskScheduler paymentMockTaskScheduler;
    private final MockWechatRefundCallbackClient callbackClient;
    private final PaymentProperties paymentProperties;

    @Override
    public void requestRefund(RefundRequest request) {
        paymentMockTaskScheduler.schedule(
                () -> callbackClient.sendRefundSuccess(request),
                Instant.now().plusMillis(paymentProperties.getMockCallbackDelayMillis())
        );
    }
}
