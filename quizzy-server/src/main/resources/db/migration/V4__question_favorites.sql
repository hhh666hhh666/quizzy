SET NAMES utf8mb4;

-- 收藏夹。取舍见 docs/adr/0030：
-- 「收藏」= 这道题至少属于某个收藏夹，不存在独立于收藏夹的收藏状态，也没有「未分组」；
-- 每个用户天然有一个默认收藏夹（`is_default = 1`），**按需创建**，所以这里不需要给存量用户补数据。

CREATE TABLE IF NOT EXISTS `favorite_folder` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL,
    `name`        VARCHAR(32)  NOT NULL,
    `is_default`  TINYINT      NOT NULL DEFAULT 0 COMMENT '1 = 默认收藏夹，不可删除；可改名',
    `create_time` DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_folder_user_name` (`user_id`, `name`),
    KEY `idx_folder_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '收藏夹';

-- ⚠️ 刻意不存「夹的最后使用时间」：夹列表的排序按下面 `create_time` 的**最大值**聚合出来，
--    存一列冗余值会在删题、改归属这些路径上慢慢漂掉（ADR 0030）。
--
-- ⚠️ 这两张新表的时间列用 **DATETIME(3)（毫秒）**，而不是库里别处的秒级 DATETIME：
--    夹列表就是按这个时间倒序的，秒级精度下「同一秒里动过两个夹」会并列、顺序随数据库心情变。
--    代价只是多几个字节，换来排序稳定。
CREATE TABLE IF NOT EXISTS `favorite_folder_question` (
    `folder_id`   BIGINT      NOT NULL,
    `question_id` BIGINT      NOT NULL,
    `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '进这个夹的时间',
    PRIMARY KEY (`folder_id`, `question_id`),
    KEY `idx_ffq_question` (`question_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '收藏夹与题目的关联';
