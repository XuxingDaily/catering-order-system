package com.catering.trade.payment.infrastructure.wechat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.catering.shared.constant.MessageConstant;
import com.catering.shared.exception.OrderBusinessException;
import com.catering.trade.payment.refund.domain.RefundRequest;
import com.catering.trade.payment.infrastructure.RefundGateway;
import com.catering.utils.WeChatPayUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 微信退款申请网关。
 */
@Component
@ConditionalOnProperty(prefix = "catering.payment", name = "provider", havingValue = "wechat")
@RequiredArgsConstructor
public class WechatRefundGateway implements RefundGateway {

    private final WeChatPayUtil weChatPayUtil;

    @Override
    public void requestRefund(RefundRequest request) {
        try {
            String response = weChatPayUtil.refund(
                    request.getOrderNumber(),
                    request.getRefundNo(),
                    request.getRefundAmount(),
                    request.getTotalAmount()
            );
            JSONObject result = JSON.parseObject(response);
            if (result == null || result.containsKey("code")) {
                throw new OrderBusinessException(MessageConstant.REFUND_REQUEST_FAILED);
            }
        } catch (OrderBusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new OrderBusinessException(MessageConstant.REFUND_REQUEST_FAILED);
        }
    }
}
