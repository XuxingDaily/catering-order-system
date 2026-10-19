package com.catering.trade.payment.refund.service;

import com.catering.trade.payment.refund.domain.RefundRequestedEvent;
import com.catering.trade.payment.infrastructure.RefundGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 保证本地取消事务提交后才向支付平台申请退款。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundRequestedEventListener {

    private final RefundGateway refundGateway;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(RefundRequestedEvent event) {
        try {
            refundGateway.requestRefund(event.getRequest());
        } catch (Exception ex) {
            // 保持退款中，避免把网络异常误判为退款失败；后续可由补偿任务重试。
            log.error(
                    "退款申请发送失败，订单号：{}，退款单号：{}",
                    event.getRequest().getOrderNumber(),
                    event.getRequest().getRefundNo(),
                    ex
            );
        }
    }
}
