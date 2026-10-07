package com.quizzy.module.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`user`")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String passwordHash;

    private String nickname;

    /** 头像 data URL（data:image/webp;base64,...）；null 表示用前端生成的默认头像。见 ADR 0028。 */
    private String avatar;

    /** token 版本号：改密码 / 退出所有设备时 +1，旧 token 立即失效。见 ADR 0027。 */
    private Integer tokenVersion;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
