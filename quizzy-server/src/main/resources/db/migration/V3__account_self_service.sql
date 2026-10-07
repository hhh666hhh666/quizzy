SET NAMES utf8mb4;

-- 账户自助（改密码 / 改昵称 / 改头像 / 注销账号）。
-- 为什么用 token_version、为什么头像存库：见 docs/adr/0027 与 docs/adr/0028。
--
-- 两条 ALTER 刻意分开写：同一条语句里引用了刚加的列（AFTER avatar）在部分 MySQL 版本上不可靠。
ALTER TABLE `user`
    ADD COLUMN `avatar` MEDIUMTEXT NULL COMMENT '头像 data URL（data:image/webp;base64,...）；NULL = 用前端生成的默认头像' AFTER `nickname`;

ALTER TABLE `user`
    ADD COLUMN `token_version` INT NOT NULL DEFAULT 0 COMMENT 'token 版本号：改密码 / 退出所有设备时 +1，旧 token 立即失效' AFTER `avatar`;
