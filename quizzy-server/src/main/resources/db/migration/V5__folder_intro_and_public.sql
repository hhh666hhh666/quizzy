SET NAMES utf8mb4;

-- 收藏夹补「简介」与「是否公开」。取舍见 docs/adr/0032。
--
-- ⚠️ `is_public` 目前是**存而不用**的字段：写出来只解决「新建 / 编辑面板里那个开关有地方落」。
--    它不产生任何可见行为——公开的收藏夹别人仍然看不到（没有发现页、没有分享链接，
--    题目可见性也没变，仍是 ADR 0030 的私有模型）。这是有意留的位，不是遗漏。
--
-- ⚠️ 都没加 NOT NULL：`is_public` 默认 0、`intro` 允许空（空 = 没有简介）。
--    存量数据的两个新列由 DEFAULT 补齐，不需要单独的回填语句。
ALTER TABLE `favorite_folder`
    ADD COLUMN `intro`     VARCHAR(200) NULL     DEFAULT NULL COMMENT '简介，最多 200 字；空 = 没填' AFTER `name`,
    ADD COLUMN `is_public` TINYINT      NOT NULL DEFAULT 0    COMMENT '1 = 公开（⚠️ 目前仅存不用，见 ADR 0032）' AFTER `is_default`;
