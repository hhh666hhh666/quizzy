package com.quizzy.module.auth.service;

import com.quizzy.common.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 头像 data URL 的校验是「前端压缩」之外唯一的一道关，所以正反两面都要钉住。
 *
 * <p>纯单元测试（{@code *Test} → surefire），不需要 Docker：{@code mvn test} 就会跑。
 */
@DisplayName("单元 · 头像 data URL")
class AvatarDataUrlTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F'};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', (byte) 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0x24, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P'};

    private static String dataUrl(String mime, byte[] bytes) {
        return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    @Test
    @DisplayName("null 与空白表示「用默认的生成头像」，返回 null 而不是报错")
    void blankMeansDefaultAvatar() {
        assertThat(AvatarDataUrl.normalize(null)).isNull();
        assertThat(AvatarDataUrl.normalize("")).isNull();
        assertThat(AvatarDataUrl.normalize("   ")).isNull();
    }

    @Test
    @DisplayName("jpeg / png / webp 都能过，并规范化回同一种形状")
    void acceptsThreeFormats() {
        assertThat(AvatarDataUrl.normalize(dataUrl("image/jpeg", JPEG))).startsWith("data:image/jpeg;base64,");
        assertThat(AvatarDataUrl.normalize(dataUrl("image/png", PNG))).startsWith("data:image/png;base64,");
        assertThat(AvatarDataUrl.normalize(dataUrl("image/webp", WEBP))).startsWith("data:image/webp;base64,");
    }

    @Test
    @DisplayName("SVG 被拒——它可以内嵌脚本，而头像会被原样渲染回浏览器")
    void rejectsSvg() {
        String svg = dataUrl("image/svg+xml", "<svg onload=\"alert(1)\"></svg>".getBytes());
        assertThatThrownBy(() -> AvatarDataUrl.normalize(svg))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("jpeg / png / webp");
    }

    @Test
    @DisplayName("声明的 MIME 与文件头不一致时拒绝——校验看内容，不看它自称是什么")
    void rejectsMismatchedDeclaration() {
        assertThatThrownBy(() -> AvatarDataUrl.normalize(dataUrl("image/png", JPEG)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不一致");
    }

    @Test
    @DisplayName("解码后超过 512 KB 拒绝")
    void rejectsOversize() {
        byte[] big = new byte[600 * 1024];
        big[0] = (byte) 0xFF;
        big[1] = (byte) 0xD8;
        big[2] = (byte) 0xFF;
        assertThatThrownBy(() -> AvatarDataUrl.normalize(dataUrl("image/jpeg", big)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("512");
    }

    @Test
    @DisplayName("不是 data URL、不是 base64、非 base64 编码的，一律拒绝")
    void rejectsGarbage() {
        assertThatThrownBy(() -> AvatarDataUrl.normalize("https://example.com/a.png"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> AvatarDataUrl.normalize("data:image/png,notbase64"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> AvatarDataUrl.normalize(dataUrl("image/png", PNG).replace("base64,", "utf8,")))
                .isInstanceOf(BusinessException.class);
    }
}
