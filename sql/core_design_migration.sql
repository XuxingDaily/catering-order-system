-- 苍穹外卖核心设计数据库迁移脚本
-- 适用：从课程原始 catering_order_system 库升级到当前项目结构。
-- 支持：MySQL 5.7 / 8.0；可重复执行。
--
-- 执行前建议：
--   1. 先备份数据库；
--   2. 先选择目标库，例如 USE catering_order_system；
--   3. 执行后检查脚本末尾的验证查询。

SET NAMES utf8mb4;

-- 使用 information_schema 判断字段和索引是否已经存在，避免重复执行时报错。
DROP PROCEDURE IF EXISTS sky_add_column_if_missing;
DROP PROCEDURE IF EXISTS sky_add_index_if_missing;

DELIMITER $$

CREATE PROCEDURE sky_add_column_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @sky_ddl = CONCAT(
                'ALTER TABLE `', p_table_name,
                '` ADD COLUMN `', p_column_name, '` ',
                p_column_definition
                       );
        PREPARE sky_stmt FROM @sky_ddl;
        EXECUTE sky_stmt;
        DEALLOCATE PREPARE sky_stmt;
    END IF;
END$$

CREATE PROCEDURE sky_add_index_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_index_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND INDEX_NAME = p_index_name
    ) THEN
        SET @sky_ddl = CONCAT(
                'ALTER TABLE `', p_table_name,
                '` ADD ', p_index_definition
                       );
        PREPARE sky_stmt FROM @sky_ddl;
        EXECUTE sky_stmt;
        DEALLOCATE PREPARE sky_stmt;
    END IF;
END$$

DELIMITER ;

-- ---------------------------------------------------------------------------
-- 1. 菜品和套餐库存
-- ---------------------------------------------------------------------------
CALL sky_add_column_if_missing(
        'dish',
        'stock',
        'INT NOT NULL DEFAULT 0 COMMENT ''可用库存'' AFTER `status`'
     );

CALL sky_add_column_if_missing(
        'setmeal',
        'stock',
        'INT NOT NULL DEFAULT 0 COMMENT ''可用库存'' AFTER `status`'
     );

-- 兼容之前手工添加成可空字段的数据库。
UPDATE dish SET stock = 0 WHERE stock IS NULL;
UPDATE setmeal SET stock = 0 WHERE stock IS NULL;

ALTER TABLE dish
    MODIFY COLUMN stock INT NOT NULL DEFAULT 0 COMMENT '可用库存';
ALTER TABLE setmeal
    MODIFY COLUMN stock INT NOT NULL DEFAULT 0 COMMENT '可用库存';

-- 注意：迁移不会猜测真实库存，历史菜品和套餐默认库存为 0，
-- 请在管理端或迁移后根据实际情况设置初始库存。

-- ---------------------------------------------------------------------------
-- 2. 下单幂等、支付、退款和超时关单字段
-- ---------------------------------------------------------------------------
CALL sky_add_column_if_missing(
        'orders',
        'transaction_id',
        'VARCHAR(64) NULL COMMENT ''支付平台交易号'' AFTER `number`'
     );

CALL sky_add_column_if_missing(
        'orders',
        'refund_status',
        'TINYINT NOT NULL DEFAULT 0 COMMENT ''退款状态：0无退款，1退款中，2退款成功，3退款失败'' AFTER `pay_status`'
     );

CALL sky_add_column_if_missing(
        'orders',
        'refund_no',
        'VARCHAR(64) NULL COMMENT ''商户退款单号'' AFTER `refund_status`'
     );

CALL sky_add_column_if_missing(
        'orders',
        'refund_time',
        'DATETIME NULL COMMENT ''退款成功时间'' AFTER `refund_no`'
     );

CALL sky_add_column_if_missing(
        'orders',
        'expire_time',
        'DATETIME NULL COMMENT ''支付过期时间'' AFTER `refund_time`'
     );

CALL sky_add_column_if_missing(
        'orders',
        'request_id',
        'VARCHAR(64) NULL COMMENT ''下单业务请求号'' AFTER `expire_time`'
     );

CALL sky_add_column_if_missing(
        'orders',
        'cancel_time',
        'DATETIME NULL COMMENT ''订单取消时间'' AFTER `rejection_reason`'
     );

-- 兼容已经存在但值为空的历史数据。
UPDATE orders
SET refund_status = 0
WHERE refund_status IS NULL;

-- 历史订单没有客户端 requestId；使用主键生成稳定且唯一的迁移值。
UPDATE orders
SET request_id = CONCAT('LEGACY_', id)
WHERE request_id IS NULL
   OR TRIM(request_id) = '';

-- 仅为尚未支付且仍待付款的历史订单补充过期时间。
UPDATE orders
SET expire_time = DATE_ADD(order_time, INTERVAL 15 MINUTE)
WHERE expire_time IS NULL
  AND order_time IS NOT NULL
  AND status = 1
  AND pay_status = 0;

ALTER TABLE orders
    MODIFY COLUMN refund_status TINYINT NOT NULL DEFAULT 0
        COMMENT '退款状态：0无退款，1退款中，2退款成功，3退款失败',
    MODIFY COLUMN request_id VARCHAR(64) NOT NULL
        COMMENT '下单业务请求号';

-- ---------------------------------------------------------------------------
-- 3. 唯一约束和任务扫描索引
-- ---------------------------------------------------------------------------
-- 如果这里因 Duplicate entry 失败，说明历史数据本身存在冲突，应先人工核对，
-- 不要为了通过迁移而直接删除订单。
CALL sky_add_index_if_missing(
        'orders',
        'uk_orders_number',
        'UNIQUE INDEX `uk_orders_number` (`number`)'
     );

-- 同一用户对同一个业务请求号只能创建一张订单，是下单幂等的最终防线。
CALL sky_add_index_if_missing(
        'orders',
        'uk_orders_user_request',
        'UNIQUE INDEX `uk_orders_user_request` (`user_id`, `request_id`)'
     );

-- 支付平台交易号和商户退款单号均应唯一；NULL 可以存在多行。
CALL sky_add_index_if_missing(
        'orders',
        'uk_orders_transaction_id',
        'UNIQUE INDEX `uk_orders_transaction_id` (`transaction_id`)'
     );

CALL sky_add_index_if_missing(
        'orders',
        'uk_orders_refund_no',
        'UNIQUE INDEX `uk_orders_refund_no` (`refund_no`)'
     );

-- 支持定时扫描“待付款且已过期”的订单。
CALL sky_add_index_if_missing(
        'orders',
        'idx_orders_timeout_scan',
        'INDEX `idx_orders_timeout_scan` (`status`, `pay_status`, `expire_time`)'
     );

-- 为后续退款补偿任务扫描长时间处于退款中的订单预留索引。
CALL sky_add_index_if_missing(
        'orders',
        'idx_orders_refund_scan',
        'INDEX `idx_orders_refund_scan` (`refund_status`, `id`)'
     );

-- 清理迁移期间使用的临时存储过程。
DROP PROCEDURE IF EXISTS sky_add_column_if_missing;
DROP PROCEDURE IF EXISTS sky_add_index_if_missing;

-- ---------------------------------------------------------------------------
-- 4. 迁移结果验证
-- ---------------------------------------------------------------------------
SELECT TABLE_NAME,
       COLUMN_NAME,
       COLUMN_TYPE,
       IS_NULLABLE,
       COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND (
        (TABLE_NAME = 'dish' AND COLUMN_NAME = 'stock')
        OR (TABLE_NAME = 'setmeal' AND COLUMN_NAME = 'stock')
        OR (TABLE_NAME = 'orders' AND COLUMN_NAME IN (
            'transaction_id', 'refund_status', 'refund_no', 'refund_time',
            'expire_time', 'request_id', 'cancel_time'
        ))
      )
ORDER BY TABLE_NAME, ORDINAL_POSITION;

SELECT TABLE_NAME,
       INDEX_NAME,
       NON_UNIQUE,
       GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS INDEX_COLUMNS
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'orders'
  AND INDEX_NAME IN (
      'uk_orders_number',
      'uk_orders_user_request',
      'uk_orders_transaction_id',
      'uk_orders_refund_no',
      'idx_orders_timeout_scan',
      'idx_orders_refund_scan'
  )
GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE
ORDER BY INDEX_NAME;

