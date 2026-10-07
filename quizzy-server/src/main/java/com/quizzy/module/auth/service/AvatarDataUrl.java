package com.quizzy.module.auth.service;

import com.quizzy.common.BusinessException;
import com.quizzy.common.ResultCode;

import java.util.Base64;
import java.util.Locale;

/**
 * 头像 data URL 的校验与规范化。存储形态与取舍见 docs/adr/0028。
 *
 * <p>这里是**唯一**一道真正的关：前端会先把图压到最长边 ≤256px 再转 webp，但前端永远不可信——
 * 所以类型按**文件头**判定（不看扩展名、也不看它声明的 MIME），并且天然拒掉 SVG
 * （SVG 可以内嵌脚本，而头像会被原样渲染回浏览器）。
 *
 * <p>规范化会**重新编码** base64：既顺手抹掉客户端可能塞进来的怪异空白，也保证存进库里的串就是
 * 我们自己拼出来的形状。
 */
final class AvatarDataUrl {

    /** 便宜的先挡一道：避免对一个超大字符串做 base64 解码。 */
    private static final int MAX_DATA_URL_CHARS = 1_000_000;

    /** 真正的上限。256px 的 webp 头像通常在 10~30KB，这里留了两个数量级的余量。 */
    private static final int MAX_DECODED_BYTES = 512 * 1024;

    private static final String PREFIX = "data:image/";

    private AvatarDataUrl() {
    }

    /**
     * 校验并规范化。返回 null 表示「用默认的生成头像」；非法输入抛 {@link BusinessException}（400）。
     */
    static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (value.length() > MAX_DATA_URL_CHARS) {
            throw invalid("头像过大，请压缩后再上传");
        }
        if (!value.startsWith(PREFIX)) {
            throw invalid("头像格式不正确");
        }
        int comma = value.indexOf(',');
        if (comma < 0) {
            throw invalid("头像格式不正确");
        }
        String[] meta = value.substring(PREFIX.length(), comma).split(";");
        String declaredMime = ("image/" + meta[0]).toLowerCase(Locale.ROOT);
        boolean base64 = meta.length > 1 && "base64".equalsIgnoreCase(meta[1]);
        if (!base64) {
            throw invalid("头像必须是 base64 编码的 data URL");
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(value.substring(comma + 1));
        } catch (IllegalArgumentException e) {
            throw invalid("头像内容不是合法的 base64");
        }
        if (bytes.length > MAX_DECODED_BYTES) {
            throw invalid("头像过大，压缩后应小于 512 KB");
        }

        String actualMime = detectMime(bytes);
        if (actualMime == null) {
            throw invalid("头像只支持 jpeg / png / webp 格式");
        }
        if (!actualMime.equals(declaredMime)) {
            // 声明与内容不一致，多半是在试探校验逻辑，直接拒绝而不是「以大度的方式接受」。
            throw invalid("头像声明的格式与实际内容不一致");
        }
        return PREFIX + meta[0].toLowerCase(Locale.ROOT) + ";base64,"
                + Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 按文件头判断图片类型，不认识的返回 null。
     */
    private static String detectMime(byte[] b) {
        if (b.length >= 3
                && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 8
                && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && (b[4] & 0xFF) == 0x0D && (b[5] & 0xFF) == 0x0A
                && (b[6] & 0xFF) == 0x1A && (b[7] & 0xFF) == 0x0A) {
            return "image/png";
        }
        if (b.length >= 12
                && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ResultCode.BAD_REQUEST, message);
    }
}
