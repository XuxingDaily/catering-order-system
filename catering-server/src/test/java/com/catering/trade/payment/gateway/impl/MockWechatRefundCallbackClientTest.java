package com.catering.trade.payment.gateway.impl;

import com.catering.properties.WeChatProperties;
import com.catering.trade.payment.config.PaymentProperties;
import com.catering.trade.payment.infrastructure.mock.MockWechatRefundCallbackClient;
import com.catering.trade.payment.refund.domain.RefundRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MockWechatRefundCallbackClientTest {

    @Test
    void shouldPostRefundNotificationToNotifyEndpoint() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        PaymentProperties properties = new PaymentProperties();
        properties.setMockRefundNotifyUrl("http://127.0.0.1:8080/notify/refundResult");
        WeChatProperties weChatProperties = new WeChatProperties();
        weChatProperties.setMchid("mock-mchid");
        MockWechatRefundCallbackClient client = new MockWechatRefundCallbackClient(
                restTemplate, properties, weChatProperties
        );

        server.expect(requestTo(properties.getMockRefundNotifyUrl()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.refundNo").value("REFUND_ORDER_001"))
                .andExpect(jsonPath("$.refundStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.refundAmount").value(1880))
                .andRespond(withSuccess("{\"code\":\"SUCCESS\"}", MediaType.APPLICATION_JSON));

        client.sendRefundSuccess(
                RefundRequest.builder()
                        .orderNumber("ORDER_001")
                        .transactionId("TRANSACTION_001")
                        .refundNo("REFUND_ORDER_001")
                        .refundAmount(new BigDecimal("18.80"))
                        .build()
        );

        server.verify();
    }
}
