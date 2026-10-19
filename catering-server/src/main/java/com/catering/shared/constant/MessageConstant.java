package com.catering.shared.constant;

/**
 * 信息提示常量类
 */
public class MessageConstant {

    public static final String PASSWORD_ERROR = "密码错误";
    public static final String ACCOUNT_NOT_FOUND = "账号不存在";
    public static final String ACCOUNT_LOCKED = "账号被锁定";
    public static final String UNKNOWN_ERROR = "未知错误";
    public static final String USER_NOT_LOGIN = "用户未登录";
    public static final String CATEGORY_BE_RELATED_BY_SETMEAL = "当前分类关联了套餐,不能删除";
    public static final String CATEGORY_BE_RELATED_BY_DISH = "当前分类关联了菜品,不能删除";
    public static final String SHOPPING_CART_IS_NULL = "购物车数据为空，不能下单";
    public static final String ADDRESS_BOOK_IS_NULL = "用户地址为空，不能下单";
    public static final String LOGIN_FAILED = "登录失败";
    public static final String UPLOAD_FAILED = "文件上传失败";
    public static final String SETMEAL_ENABLE_FAILED = "套餐内包含未启售菜品，无法启售";
    public static final String PASSWORD_EDIT_FAILED = "密码修改失败";
    public static final String DISH_ON_SALE = "起售中的菜品不能删除";
    public static final String SETMEAL_ON_SALE = "起售中的套餐不能删除";
    public static final String DISH_BE_RELATED_BY_SETMEAL = "当前菜品关联了套餐,不能删除";
    public static final String ORDER_STATUS_ERROR = "订单状态错误";
    public static final String ORDER_NOT_FOUND = "订单不存在";
    public static final String ORDER_TIMEOUT = "用户订单超时，自动取消";
    public static final String USER_CANCEL_ORDER = "用户取消订单";
    public static final String ORDER_STATUS_REQUIRED = "订单状态不能为空";
    public static final String ORDER_STATUS_UNKNOWN = "未知订单状态";
    public static final String ORDER_TRANSITION_NOT_ALLOWED = "当前订单状态不允许执行该操作";
    public static final String ORDER_CANCEL_EVENT_INVALID = "当前事件不属于取消订单操作";
    public static final String REFUND_RESULT_INVALID = "退款结果状态不合法";
    public static final String REFUND_NO_MISSING = "订单缺少商户退款单号";
    public static final String REFUND_AMOUNT_INVALID = "退款金额与订单金额不一致";
    public static final String REFUND_ORDER_INVALID = "退款通知与原订单信息不一致";
    public static final String REFUND_REQUEST_FAILED = "退款申请失败";
    public static final String REFUND_NOTIFY_PROCESS_FAILED = "退款回调处理失败";
    public static final String ORDER_TRANSACTION_ID_MISSING = "已支付订单缺少微信支付交易号";

    public static final String PAYMENT_NOTIFY_HEADER_MISSING = "微信支付回调请求头不完整";
    public static final String PAYMENT_NOTIFY_TIMESTAMP_INVALID = "微信支付回调时间戳不合法";
    public static final String PAYMENT_NOTIFY_SIGNATURE_INVALID = "微信支付回调验签失败";
    public static final String PAYMENT_NOTIFY_CONFIG_MISSING = "微信支付回调配置不完整";
    public static final String PAYMENT_NOTIFY_BODY_INVALID = "微信支付回调数据不合法";
    public static final String PAYMENT_NOTIFY_EVENT_INVALID = "微信支付回调事件类型不合法";
    public static final String PAYMENT_NOTIFY_MERCHANT_INVALID = "微信支付回调商户信息不匹配";
    public static final String PAYMENT_NOTIFY_AMOUNT_INVALID = "支付金额与订单金额不一致";
    public static final String PAYMENT_NOTIFY_PROCESS_FAILED = "微信支付回调处理失败";

    public static final String DISH_STOCK_NOT_ENOUGH = "菜品库存不足";
    public static final String SETMEAL_STOCK_NOT_ENOUGH = "套餐库存不足";
    public static final String INVENTORY_RELEASE_FAILED = "库存释放失败";
    public static final String INVENTORY_QUANTITY_INVALID = "商品数量不合法";
    public static final String INVENTORY_ITEM_INVALID = "订单明细缺少商品信息";

}
