package com.catering.trade.payment.infrastructure.wechat;

import com.alibaba.fastjson.JSONObject;
import com.catering.shared.exception.PaymentBusinessException;
import com.catering.trade.payment.infrastructure.PaymentGateway;
import com.catering.utils.WeChatPayUtil;
import com.catering.vo.OrderPaymentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(
        prefix = "catering.payment",
        name = "provider",
        havingValue = "wechat"
)
@RequiredArgsConstructor
public class WechatPaymentGateway implements PaymentGateway {

    private final WeChatPayUtil weChatPayUtil;

    @Override
    public OrderPaymentVO createPayment(String orderNumber, BigDecimal amount, String description, String openid) {
        try {
            JSONObject result = weChatPayUtil.pay(
                    orderNumber,
                    amount,
                    description,
                    openid
            );

            return OrderPaymentVO.builder()
                    .nonceStr(result.getString("nonceStr"))
                    .timeStamp(result.getString("timeStamp"))
                    .packageStr(result.getString("package"))
                    .signType(result.getString("signType"))
                    .paySign(result.getString("paySign"))
                    .build();
        } catch (Exception e) {
            throw new PaymentBusinessException("创建微信支付订单失败");
        }
    }
}
