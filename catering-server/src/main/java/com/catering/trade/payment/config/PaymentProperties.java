package com.catering.trade.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付通道及本地 Mock 回调配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "catering.payment")
public class PaymentProperties {

    private String provider = "mock";
    private String mockNotifyUrl = "http://127.0.0.1:8080/notify/paySuccess";
    private String mockRefundNotifyUrl = "http://127.0.0.1:8080/notify/refundResult";
    private long mockCallbackDelayMillis = 1000L;
}
