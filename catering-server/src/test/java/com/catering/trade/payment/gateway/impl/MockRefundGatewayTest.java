package com.catering.trade.payment.gateway.impl;

import com.catering.trade.payment.config.PaymentProperties;
import com.catering.trade.payment.infrastructure.mock.MockRefundGateway;
import com.catering.trade.payment.infrastructure.mock.MockWechatRefundCallbackClient;
import com.catering.trade.payment.refund.domain.RefundRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockRefundGatewayTest {

    @Mock
    private TaskScheduler paymentMockTaskScheduler;

    @Mock
    private MockWechatRefundCallbackClient callbackClient;

    @Mock
    private PaymentProperties paymentProperties;

    @InjectMocks
    private MockRefundGateway refundGateway;

    @Test
    void shouldScheduleAsynchronousRefundCallback() {
        when(paymentProperties.getMockCallbackDelayMillis()).thenReturn(1000L);
        RefundRequest request = RefundRequest.builder()
                .orderNumber("ORDER_001")
                .refundNo("REFUND_ORDER_001")
                .refundAmount(new BigDecimal("18.80"))
                .build();

        refundGateway.requestRefund(request);

        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(paymentMockTaskScheduler).schedule(taskCaptor.capture(), any(Instant.class));
        taskCaptor.getValue().run();
        verify(callbackClient).sendRefundSuccess(request);
    }
}
