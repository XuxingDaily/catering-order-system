package com.catering.trade.payment.gateway.impl;

import com.catering.trade.payment.infrastructure.mock.MockPaymentNotificationGateway;
import com.catering.trade.payment.pay.domain.PaymentNotification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MockPaymentNotificationGatewayTest {

    private final MockPaymentNotificationGateway gateway = new MockPaymentNotificationGateway();

    @Test
    void shouldParseMockHttpCallbackBody() {
        String body = "{"
                + "\"eventType\":\"TRANSACTION.SUCCESS\","
                + "\"appId\":\"mock-appid\","
                + "\"mchId\":\"mock-mchid\","
                + "\"outTradeNo\":\"ORDER_001\","
                + "\"transactionId\":\"MOCK_TRANSACTION_001\","
                + "\"tradeState\":\"SUCCESS\","
                + "\"totalAmount\":1880,"
                + "\"currency\":\"CNY\""
                + "}";

        PaymentNotification result = gateway.verifyAndDecrypt(
                "MOCK_SERIAL",
                "1",
                "MOCK_NONCE",
                "MOCK_SIGNATURE",
                body
        );

        assertEquals("ORDER_001", result.getOutTradeNo());
        assertEquals("MOCK_TRANSACTION_001", result.getTransactionId());
        assertEquals(1880, result.getTotalAmount());
    }
}
