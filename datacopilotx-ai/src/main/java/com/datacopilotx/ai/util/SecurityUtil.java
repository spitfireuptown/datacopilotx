package com.datacopilotx.ai.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类，用于获取当前登录用户信息
 */
public class SecurityUtil {

    /**
     * 获取当前登录用户的ID
     * @return 用户ID，如果未登录则返回null
     */
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        // 认证成功后，principal即为用户ID（在JwtAuthenticationFilter中设置）
        Object principal = authentication.getPrincipal();
        if (principal instanceof String) {
            return (String) principal;
        }
        
        return null;
    }

    /**
     * 获取当前登录用户的角色
     * @return 角色值：0-超级管理员，1-管理员，2-普通用户
     */
    public static Integer getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return 2; // 默认普通用户
        }
        
        // 从Authority中获取角色
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String authorityStr = authority.getAuthority();
            if (authorityStr.startsWith("ROLE_")) {
                try {
                    return Integer.parseInt(authorityStr.substring(5));
                } catch (NumberFormatException e) {
                    return 2;
                }
            }
        }
        
        return 2; // 默认普通用户
    }

    /**
     * 判断当前用户是否已登录
     * @return true表示已登录
     */
    public static boolean isLoggedIn() {
        return getCurrentUserId() != null;
    }

    /**
     * 判断当前用户是否为超级管理员
     * @return true表示是超级管理员
     */
    public static boolean isSuperAdmin() {
        return getCurrentUserRole() == 0;
    }

    /**
     * 判断当前用户是否为管理员（包括超级管理员）
     * @return true表示是管理员
     */
    public static boolean isAdmin() {
        Integer role = getCurrentUserRole();
        return role == 0 || role == 1;
    }
}