SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `username`      VARCHAR(64)  NOT NULL,
    `password_hash` VARCHAR(100) NOT NULL,
    `nickname`      VARCHAR(64)  NOT NULL DEFAULT '',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户';

CREATE TABLE IF NOT EXISTS `category` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(64) NOT NULL,
    `sort`        INT         NOT NULL DEFAULT 0,
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '分类（平级，全局共享）';

CREATE TABLE IF NOT EXISTS `tag` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(64) NOT NULL,
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tag_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '标签（全局共享）';

CREATE TABLE IF NOT EXISTS `question` (
    `id`           BIGINT      NOT NULL AUTO_INCREMENT,
    `type`         VARCHAR(16) NOT NULL COMMENT 'SINGLE | MULTI | JUDGE',
    `stem`         TEXT        NOT NULL COMMENT 'Markdown',
    `analysis`     TEXT        NULL COMMENT 'Markdown',
    `difficulty`   VARCHAR(16) NOT NULL DEFAULT 'MEDIUM' COMMENT 'EASY | MEDIUM | HARD',
    `answer`       VARCHAR(64) NOT NULL DEFAULT '' COMMENT '正确答案选项 label，多个用逗号分隔，如 A,C',
    `score`        INT         NOT NULL DEFAULT 1,
    `category_id`  BIGINT      NULL,
    `owner_id`     BIGINT      NULL COMMENT 'NULL = 公开题（只读）',
    `deleted`      TINYINT     NOT NULL DEFAULT 0,
    `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_question_owner_deleted` (`owner_id`, `deleted`),
    KEY `idx_question_category` (`category_id`),
    KEY `idx_question_type` (`type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '题目';

CREATE TABLE IF NOT EXISTS `question_option` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `question_id` BIGINT       NOT NULL,
    `label`       VARCHAR(4)   NOT NULL COMMENT 'A ~ F',
    `content`     VARCHAR(500) NOT NULL,
    `sort`        INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_option_question` (`question_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '题目选项';

CREATE TABLE IF NOT EXISTS `question_tag` (
    `question_id` BIGINT NOT NULL,
    `tag_id`      BIGINT NOT NULL,
    PRIMARY KEY (`question_id`, `tag_id`),
    KEY `idx_qt_tag` (`tag_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '题目标签关联';

CREATE TABLE IF NOT EXISTS `question_stat` (
    `id`                  BIGINT   NOT NULL AUTO_INCREMENT,
    `user_id`             BIGINT   NOT NULL,
    `question_id`         BIGINT   NOT NULL,
    `answer_count`        INT      NOT NULL DEFAULT 0,
    `correct_count`       INT      NOT NULL DEFAULT 0,
    `consecutive_correct` INT      NOT NULL DEFAULT 0,
    `last_correct`        TINYINT  NOT NULL DEFAULT 0,
    `in_wrong_book`       TINYINT  NOT NULL DEFAULT 0,
    `last_answer_time`    DATETIME NULL,
    `update_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stat_user_question` (`user_id`, `question_id`),
    KEY `idx_stat_wrong` (`user_id`, `in_wrong_book`, `last_answer_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户题目作答统计';

CREATE TABLE IF NOT EXISTS `paper` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `title`          VARCHAR(128) NOT NULL,
    `description`    VARCHAR(500) NULL,
    `mode`           VARCHAR(16)  NOT NULL COMMENT 'FIXED | RULE',
    `rule_json`      TEXT         NULL COMMENT '仅 RULE 模式',
    `question_count` INT          NOT NULL DEFAULT 0,
    `owner_id`       BIGINT       NOT NULL,
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_paper_owner` (`owner_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '试卷';

CREATE TABLE IF NOT EXISTS `paper_question` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `paper_id`    BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `sort`        INT    NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_paper_question` (`paper_id`, `question_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '固定卷题目列表';

CREATE TABLE IF NOT EXISTS `quiz_session` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`        BIGINT       NOT NULL,
    `paper_id`       BIGINT       NULL,
    `source_type`    VARCHAR(16)  NOT NULL COMMENT 'PAPER | QUICK | WRONG_BOOK',
    `title`          VARCHAR(128) NOT NULL DEFAULT '',
    `question_count` INT          NOT NULL DEFAULT 0,
    `current_index`  INT          NOT NULL DEFAULT 0,
    `total_score`    INT          NOT NULL DEFAULT 0,
    `obtained_score` INT          NOT NULL DEFAULT 0,
    `status`         VARCHAR(16)  NOT NULL DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS | COMPLETED | ABANDONED',
    `start_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `finish_time`    DATETIME     NULL,
    PRIMARY KEY (`id`),
    KEY `idx_session_user_status` (`user_id`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '答题会话';

CREATE TABLE IF NOT EXISTS `quiz_answer` (
    `id`           BIGINT      NOT NULL AUTO_INCREMENT,
    `session_id`   BIGINT      NOT NULL,
    `question_id`  BIGINT      NOT NULL,
    `user_answer`  VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'A 或 A,C',
    `is_correct`   TINYINT     NOT NULL DEFAULT 0,
    `score`        INT         NOT NULL DEFAULT 0,
    `sort`         INT         NOT NULL DEFAULT 0,
    `answered_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_answer_session_question` (`session_id`, `question_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '会话作答记录';
