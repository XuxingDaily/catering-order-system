package com.catering.trade.payment.gateway.impl;

import com.catering.properties.WeChatProperties;
import com.catering.trade.payment.config.PaymentProperties;
import com.catering.trade.payment.infrastructure.mock.MockWechatCallbackClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MockWechatCallbackClientTest {

    @Test
    void shouldPostPaymentNotificationToNotifyEndpoint() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

        PaymentProperties paymentProperties = new PaymentProperties();
        paymentProperties.setMockNotifyUrl("http://127.0.0.1:8080/notify/paySuccess");

        WeChatProperties weChatProperties = new WeChatProperties();
        weChatProperties.setAppid("mock-appid");
        weChatProperties.setMchid("mock-mchid");

        MockWechatCallbackClient client = new MockWechatCallbackClient(
                restTemplate,
                paymentProperties,
                weChatProperties
        );

        server.expect(requestTo(paymentProperties.getMockNotifyUrl()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Wechatpay-Serial", "MOCK_SERIAL"))
                .andExpect(jsonPath("$.outTradeNo").value("ORDER_001"))
                .andExpect(jsonPath("$.transactionId").value("MOCK_TRANSACTION_001"))
                .andExpect(jsonPath("$.totalAmount").value(1880))
                .andRespond(withSuccess("{\"code\":\"SUCCESS\"}", MediaType.APPLICATION_JSON));

        client.sendPaymentSuccess(
                "ORDER_001",
                "MOCK_TRANSACTION_001",
                new BigDecimal("18.80")
        );

        server.verify();
    }
}
