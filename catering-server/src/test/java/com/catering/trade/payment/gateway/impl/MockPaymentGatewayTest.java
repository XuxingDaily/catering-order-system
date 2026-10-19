package com.catering.trade.payment.gateway.impl;

import com.catering.trade.payment.config.PaymentProperties;
import com.catering.trade.payment.infrastructure.mock.MockPaymentGateway;
import com.catering.trade.payment.infrastructure.mock.MockWechatCallbackClient;
import com.catering.vo.OrderPaymentVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockPaymentGatewayTest {

    @Mock
    private TaskScheduler paymentMockTaskScheduler;

    @Mock
    private MockWechatCallbackClient mockWechatCallbackClient;

    @Mock
    private PaymentProperties paymentProperties;

    @InjectMocks
    private MockPaymentGateway mockPaymentGateway;

    @Test
    void createPaymentShouldReturnParametersAndScheduleHttpCallback() {
        when(paymentProperties.getMockCallbackDelayMillis()).thenReturn(1000L);

        OrderPaymentVO result = mockPaymentGateway.createPayment(
                "ORDER_001",
                new BigDecimal("18.80"),
                "餐饮订单",
                "mock-openid"
        );

        assertNotNull(result.getNonceStr());
        assertEquals("RSA", result.getSignType());
        assertTrue(result.getPackageStr().startsWith("prepay_id=mock_"));
        assertEquals("MOCK_PAY_SIGN_ORDER_001", result.getPaySign());

        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(paymentMockTaskScheduler).schedule(callbackCaptor.capture(), any(Instant.class));

        callbackCaptor.getValue().run();
        verify(mockWechatCallbackClient).sendPaymentSuccess(
                org.mockito.ArgumentMatchers.eq("ORDER_001"),
                org.mockito.ArgumentMatchers.startsWith("MOCK_TRANSACTION_"),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("18.80"))
        );
    }
}
