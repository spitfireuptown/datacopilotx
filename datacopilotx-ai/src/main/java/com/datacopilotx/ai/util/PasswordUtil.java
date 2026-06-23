package com.datacopilotx.ai.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码工具类
 * 提供密码加密和验证功能
 */
public class PasswordUtil {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /**
     * 加密密码
     *
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    public static String encode(String rawPassword) {
        return PASSWORD_ENCODER.encode(rawPassword);
    }

    /**
     * 验证密码
     *
     * @param rawPassword     原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return PASSWORD_ENCODER.matches(rawPassword, encodedPassword);
    }

    /**
     * 生成默认管理员密码的加密值
     *
     * @param password 原始密码
     * @return 加密后的密码
     */
    public static String generateAdminPassword(String password) {
        return encode(password);
    }

    /**
     * 主方法，用于生成密码加密值
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 生成默认密码 "datacopilotx" 的加密值
        String defaultPassword = "datacopilotx";
        String encodedPassword = encode(defaultPassword);
        
        System.out.println("原始密码: " + defaultPassword);
        System.out.println("加密后密码: " + encodedPassword);
        System.out.println("密码长度: " + encodedPassword.length());
        
        // 验证密码
        boolean matches = matches(defaultPassword, encodedPassword);
        System.out.println("密码验证: " + (matches ? "成功" : "失败"));
        
        // 如果提供了命令行参数，则加密指定的密码
        if (args.length > 0) {
            String customPassword = args[0];
            String customEncoded = encode(customPassword);
            System.out.println("\n自定义密码加密:");
            System.out.println("原始密码: " + customPassword);
            System.out.println("加密后密码: " + customEncoded);
        }
    }
}