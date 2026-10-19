package com.catering.trade.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.catering.shared.constant.MessageConstant;
import com.catering.customer.address.mapper.AddressBookMapper;
import com.catering.identity.user.mapper.UserMapper;
import com.catering.shared.context.BaseContext;
import com.catering.dto.*;
import com.catering.entity.*;
import com.catering.shared.exception.OrderBusinessException;
import com.catering.shared.result.PageResult;
import com.catering.trade.inventory.service.InventoryService;
import com.catering.trade.order.domain.OrderEvent;
import com.catering.trade.order.domain.OrderStatus;
import com.catering.trade.order.mapper.OrderDetailMapper;
import com.catering.trade.order.mapper.OrderMapper;
import com.catering.trade.order.service.OrderService;
import com.catering.trade.order.service.OrderCancellationService;
import com.catering.trade.cart.mapper.ShoppingCartMapper;
import com.catering.trade.order.service.OrderStateService;
import com.catering.trade.payment.pay.domain.PayStatus;
import com.catering.trade.payment.refund.domain.RefundStatus;
import com.catering.trade.payment.infrastructure.PaymentGateway;
import com.catering.vo.*;
import com.catering.infrastructure.websocket.WebSocketServer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单应用服务实现，负责下单、查询、支付入口以及订单履约流程的协调。
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;

    private final OrderDetailMapper orderDetailMapper;

    private final AddressBookMapper addressBookMapper;

    private final ShoppingCartMapper shoppingCartMapper;

    private final UserMapper userMapper;

    private final WebSocketServer webSocketServer;

    private final OrderStateService orderStateService;

    private final OrderCancellationService orderCancellationService;

    private final InventoryService inventoryService;

    private final PaymentGateway paymentGateway;

    /**
     * 提交订单，并通过 {@code userId + requestId} 实现重复请求幂等。
     *
     * @param ordersSubmitDTO 提交的订单信息
     * @return 新订单或幂等命中的原订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.selectById(userId);

        // 同一次下单及其重试必须复用 requestId；数据库唯一索引负责并发最终兜底。
        String requestId = ordersSubmitDTO.getRequestId();
        if (requestId == null || requestId.trim().isEmpty()) {
            // TODO：前端接入业务请求号后改为必传；当前生成 UUID 以兼容现有页面。
            requestId = UUID.randomUUID().toString().replace("-", "");
        }
        // 快速返回已经创建的订单；后续可用 Redis SETNX 增加前置拦截。
        Orders existingOrder = findByRequestId(userId, requestId, false);
        if (existingOrder != null) {
            return buildSubmitVO(existingOrder);
        }

        // 根据 ordersSubmitDTO.addressBookId 搜索地址记录
        AddressBook addressBook = addressBookMapper.selectById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new OrderBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        String address = addressBook.getProvinceName() + " " +
                         addressBook.getCityName() + " " +
                         addressBook.getDistrictName() + " " +
                         addressBook.getDetail();

        // 1. 读取购物车
        // 搜索购物车记录是否为空
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.selectListByUserId(userId);
        if (shoppingCarts == null || shoppingCarts.isEmpty()) {
            throw new OrderBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 2. 构造并插入订单项
        // 新的订单记录插入 orders
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        LocalDateTime now = LocalDateTime.now();
        orders.setOrderTime(now);
        orders.setExpireTime(now.plusMinutes(15));
        orders.setRequestId(requestId);
        orders.setPayStatus(PayStatus.UN_PAID.getCode());
        orders.setRefundStatus(RefundStatus.NONE.getCode());
        orders.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        orders.setNumber(UUID.randomUUID().toString().replace("-", "")); // 设置商户订单号
        orders.setAddress(address);
        orders.setUserName(user.getName());
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);
        try {
            orderMapper.insert(orders);
        } catch (DuplicateKeyException ex) {
            // 唯一索引 (user_id, request_id) 是并发重复提交时的最后一道防线。
            Orders concurrentOrder = findByRequestId(userId, requestId, true);
            if (concurrentOrder != null) {
                return buildSubmitVO(concurrentOrder);
            }
            throw ex;
        }

        // 3. 构造订单项目项
        // 订单主表、明细表及购物车清理处于同一事务，任何一步失败都整体回滚。
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (ShoppingCart shoppingCart : shoppingCarts) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);
        }

        // 4. 扣除库存，如果量不足则报错
        inventoryService.deduct(orderDetails);

        // 5. 批量插入订单详细项
        orderDetailMapper.insertBatch(orderDetails);

        // 6. 清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        // 封装 VO 返回
        return buildSubmitVO(orders);
    }

    /**
     * 生成支付参数；当前使用模拟微信预下单并立即触发模拟支付成功回调。
     *
     * @param ordersPaymentDTO 商户订单号和支付方式
     * @return 前端拉起支付所需参数
     */
    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        Long userId = BaseContext.getCurrentId();
        Orders order = orderMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getNumber, ordersPaymentDTO.getOrderNumber())
                        .eq(Orders::getUserId, userId)
        );
        // 同时使用用户 ID 查询，避免用户支付他人的订单。
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 只有待支付且未支付的订单能够创建预支付交易。
        if (!Objects.equals(order.getStatus(), OrderStatus.PENDING_PAYMENT.getCode())
                || !Objects.equals(order.getPayStatus(), PayStatus.UN_PAID.getCode())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 支付请求到达时订单已经过期，则先尝试关单再返回超时异常。
        if (order.getExpireTime() != null && !order.getExpireTime().isAfter(LocalDateTime.now())) {
            orderCancellationService.cancelByTimeout(order.getId());
            throw new OrderBusinessException(MessageConstant.ORDER_TIMEOUT);
        }

        // 小程序支付需要付款用户的 openid
        User user = userMapper.selectById(order.getUserId());
        if (user == null || user.getOpenid() == null) {
            throw new OrderBusinessException(MessageConstant.USER_NOT_LOGIN);
        }

        return paymentGateway.createPayment(
                order.getNumber(),
                order.getAmount(),
                "餐饮订单",
                user.getOpenid()
        );
    }

    /**
     * 处理支付成功结果；重复微信回调不会重复发送来单通知。
     *
     * @param outTradeNo 商户订单号
     * @param transactionId 微信支付交易号
     */
    @Override
    public void paySuccess(String outTradeNo, String transactionId) {
        boolean firstNotification = orderStateService.paySuccess(
                outTradeNo,
                transactionId,
                LocalDateTime.now()
        );
        if (!firstNotification) {
            return;
        }

        // 基于 WebSocket 向管理端发送消息
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", 1); // 1 表示来单提醒，2 表示客户催单
        msg.put("content", "订单号：" + outTradeNo);

        String msgStr = JSON.toJSONString(msg);
        webSocketServer.sendMessageToAllClient(msgStr);
    }

    /**
     * 分页查询订单，并组装订单明细与菜品摘要。
     *
     * @param ordersPageQueryDTO 分页及筛选条件
     * @return 订单分页结果
     */
    @Override
    public PageResult getHistoryOrder(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> ordersPage = orderMapper.selectPageByCondition(ordersPageQueryDTO);

        List<OrderVO> orderVOList = new ArrayList<>();
        if (ordersPage == null || ordersPage.getTotal() == 0) {
            return new PageResult(0, orderVOList);
        }

        for (Orders orders : ordersPage) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(orders, orderVO);

            List<OrderDetail> orderDetails = orderDetailMapper.selectByOrderId(orders.getId());
            orderVO.setOrderDetailList(orderDetails);

            List<String> orderDishs = orderDetails.stream()
                    .map(orderDetail -> orderDetail.getName() + "*" + orderDetail.getNumber() + ";")
                    .collect(Collectors.toList());
            orderVO.setOrderDishes(String.join("", orderDishs));

            orderVOList.add(orderVO);
        }

        return new PageResult(ordersPage.getTotal(), orderVOList);
    }

    /**
     * 用户取消自己的订单，具体校验、退款和库存编排交给取消服务。
     *
     * @param orderId 订单 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelByUser(Long orderId) {
        orderCancellationService.cancelByUser(orderId, BaseContext.getCurrentId());
    }

    /**
     * 管理员取消订单，管理员场景不进行订单归属校验。
     *
     * @param ordersCancelDTO 订单 ID 与取消原因
     */
    @Override
    public void cancelByAdmin(OrdersCancelDTO ordersCancelDTO) {
        orderCancellationService.cancelByAdmin(
                ordersCancelDTO.getId(),
                ordersCancelDTO.getCancelReason()
        );
    }

    /**
     * 商家拒单，并对已支付订单执行退款编排。
     *
     * @param ordersRejectionDTO 订单 ID 与拒单原因
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        orderCancellationService.rejectByMerchant(
                ordersRejectionDTO.getId(),
                ordersRejectionDTO.getRejectionReason()
        );
    }

    /**
     * 商家接单，将待接单订单迁移为已接单。
     *
     * @param ordersConfirmDTO 订单 ID
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        orderStateService.transition(
                ordersConfirmDTO.getId(),
                OrderEvent.MERCHANT_CONFIRM
        );
    }

    /**
     * 统计管理端工作台关注的订单状态数量。
     *
     * @return 待接单、待派送和配送中的订单数量
     */
    @Override
    public OrderStatisticsVO statisticsByStatus() {
        List<OrderStatusCountDTO> map = orderMapper.countByStatus();

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();

        for (OrderStatusCountDTO orderStatusCountDTO : map) {
            if (Objects.equals(orderStatusCountDTO.getStatus(), OrderStatus.CONFIRMED.getCode())) {
                orderStatisticsVO.setConfirmed(orderStatusCountDTO.getCount());
            } else if (Objects.equals(orderStatusCountDTO.getStatus(), OrderStatus.DELIVERY_IN_PROGRESS.getCode())) {
                orderStatisticsVO.setDeliveryInProgress(orderStatusCountDTO.getCount());
            } else if (Objects.equals(orderStatusCountDTO.getStatus(), OrderStatus.TO_BE_CONFIRMED.getCode())) {
                orderStatisticsVO.setToBeConfirmed(orderStatusCountDTO.getCount());
            }
        }

        return orderStatisticsVO;
    }

    /**
     * 根据订单 ID 查询订单及其明细。
     *
     * @param id 订单 ID
     * @return 订单详情
     */
    @Override
    public OrderVO getById(Long id) {
        Orders orders = orderMapper.selectById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        List<OrderDetail> orderDetails = orderDetailMapper.selectByOrderId(id);

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetails);

        return orderVO;
    }

    /**
     * 将当前用户的历史订单明细重新加入购物车。
     *
     * @param id 原订单 ID
     */
    @Override
    public void repetitionById(Long id) {
        Long userId = BaseContext.getCurrentId();

        Orders order = orderMapper.selectById(id);
        if (order == null || !Objects.equals(order.getUserId(), userId)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetails = orderDetailMapper.selectByOrderId(id);

        // 将订单详情项转换为购物车对象
        List<ShoppingCart> shoppingCarts = orderDetails.stream().map(orderDetail -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());

        // 添加购物车对象到数据库
        shoppingCartMapper.insertBatch(shoppingCarts);
    }

    /**
     * 将已接单订单迁移为配送中。
     *
     * @param id 订单 ID
     */
    @Override
    public void delivery(Long id) {
        orderStateService.transition(id, OrderEvent.START_DELIVERY);
    }

    /**
     * 完成配送中的订单并记录送达时间。
     *
     * @param id 订单 ID
     */
    @Override
    public void complete(Long id) {
        orderStateService.complete(id, LocalDateTime.now());
    }

    /**
     * 向管理端广播用户催单消息。
     *
     * @param id 订单 ID
     */
    @Override
    public void reminder(Long id) {
        Orders ordersDB = orderMapper.selectById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Map<String, Object> msg = new HashMap<>();
        msg.put("type", 2);
        msg.put("orderId", id);
        msg.put("content", "订单号：" + ordersDB.getNumber());

        webSocketServer.sendMessageToAllClient(JSON.toJSONString(msg));
    }

    /**
     * 按用户和业务请求号查询订单；并发唯一键冲突后使用锁定读获取已创建订单。
     */
    private Orders findByRequestId(Long userId, String requestId, boolean forUpdate) {
        LambdaQueryWrapper<Orders> wrapper =  Wrappers.<Orders>lambdaQuery()
                                        .eq(Orders::getUserId, userId)
                                        .eq(Orders::getRequestId, requestId);
        if (forUpdate) {
            wrapper.last("FOR UPDATE");
        }
        return orderMapper.selectOne(wrapper);
    }

    /**
     * 将订单实体转换为下单结果。
     */
    private OrderSubmitVO buildSubmitVO(Orders orders) {
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
    }

}
