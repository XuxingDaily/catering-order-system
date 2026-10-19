package com.catering.trade.payment.controller;

import com.catering.shared.constant.MessageConstant;
import com.catering.shared.exception.PaymentNotifyException;
import com.catering.trade.payment.pay.domain.PaymentNotification;
import com.catering.trade.payment.infrastructure.PaymentNotificationGateway;
import com.catering.trade.payment.pay.controller.PayNotifyController;
import com.catering.trade.payment.pay.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayNotifyControllerTest {

    @Mock
    private PaymentNotificationGateway paymentNotificationGateway;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PayNotifyController controller;

    @Test
    void successfulNotificationShouldReturnWechatSuccessResponse() {
        PaymentNotification notification = PaymentNotification.builder()
                .outTradeNo("ORDER_001")
                .transactionId("WX_TRANSACTION_001")
                .build();
        when(paymentNotificationGateway.verifyAndDecrypt("serial", "timestamp", "nonce", "signature", "body"))
                .thenReturn(notification);

        ResponseEntity<Map<String, String>> response = controller.paySuccessNotify(mockRequest("body"));

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("SUCCESS", response.getBody().get("code"));
        verify(paymentService).handlePaymentSuccess(notification);
    }

    @Test
    void processingFailureShouldReturnNon2xxForWechatRetry() {
        PaymentNotification notification = PaymentNotification.builder()
                .outTradeNo("ORDER_001")
                .transactionId("WX_TRANSACTION_001")
                .build();
        when(paymentNotificationGateway.verifyAndDecrypt("serial", "timestamp", "nonce", "signature", "body"))
                .thenReturn(notification);
        doThrow(new PaymentNotifyException(MessageConstant.PAYMENT_NOTIFY_AMOUNT_INVALID))
                .when(paymentService).handlePaymentSuccess(notification);

        ResponseEntity<Map<String, String>> response = controller.paySuccessNotify(mockRequest("body"));

        assertEquals(500, response.getStatusCodeValue());
        assertEquals("FAIL", response.getBody().get("code"));
        assertEquals(MessageConstant.PAYMENT_NOTIFY_PROCESS_FAILED, response.getBody().get("message"));
    }

    private MockHttpServletRequest mockRequest(String body) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Wechatpay-Serial", "serial");
        request.addHeader("Wechatpay-Timestamp", "timestamp");
        request.addHeader("Wechatpay-Nonce", "nonce");
        request.addHeader("Wechatpay-Signature", "signature");
        request.setContent(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        request.setContentType("application/json");
        return request;
    }
}
