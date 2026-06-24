package com.datacopilotx.ai.controller;

import com.datacopilotx.ai.config.security.JwtTokenProvider;
import com.datacopilotx.ai.controller.form.LoginForm;
import com.datacopilotx.ai.controller.form.RegisterForm;
import com.datacopilotx.ai.controller.form.UserForm;
import com.datacopilotx.ai.domian.vo.UserInfoVo;
import com.datacopilotx.ai.service.AuthService;
import com.datacopilotx.common.result.WebResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public WebResult<Map<String, Object>> login(@Validated @RequestBody LoginForm loginForm) {
        String token = authService.login(loginForm.getUsername(), loginForm.getPassword());
        String userId = jwtTokenProvider.getUserIdFromToken(token);
        UserInfoVo userInfo = authService.getCurrentUserInfo(userId);

        return WebResult.success(Map.of(
                "token", token,
                "userInfo", userInfo
        ));
    }

    @PostMapping("/register")
    public WebResult<Void> register(@Validated @RequestBody RegisterForm registerForm) {
        authService.register(
                registerForm.getUsername(),
                registerForm.getPassword(),
                registerForm.getNickname(),
                registerForm.getEmail(),
                registerForm.getPhone()
        );

        return WebResult.success();
    }

    @PostMapping("/logout")
    public WebResult<Void> logout() {
        return WebResult.success();
    }

    @PostMapping("/user/reset-password")
    public WebResult<Void> resetPassword(@RequestBody Map<String, Object> body, @RequestHeader("Authorization") String authorization) {
        checkSuperAdmin(authorization);
        String userId = (String) body.get("userId");
        authService.resetPassword(userId);

        return WebResult.success();
    }

    @GetMapping("/user/info")
    public WebResult<UserInfoVo> getCurrentUserInfo(@RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        String userId = jwtTokenProvider.getUserIdFromToken(token);
        UserInfoVo userInfo = authService.getCurrentUserInfo(userId);

        return WebResult.success(userInfo);
    }

    @PostMapping("/user/change-password")
    public WebResult<Void> changePassword(
            @RequestHeader("Authorization") String authorization,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        String token = authorization.replace("Bearer ", "");
        String userId = jwtTokenProvider.getUserIdFromToken(token);
        authService.changePassword(userId, oldPassword, newPassword);

        return WebResult.success();
    }

    @GetMapping("/user/list")
    public WebResult<Map<String, Object>> getUserList(
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestHeader("Authorization") String authorization) {
        checkSuperAdmin(authorization);

        return WebResult.success(authService.getUserList(username, page, size));
    }

    @GetMapping("/user/{userId}")
    public WebResult<UserInfoVo> getUserById(@PathVariable String userId, @RequestHeader("Authorization") String authorization) {
        checkSuperAdmin(authorization);

        return WebResult.success(authService.getUserById(userId));
    }

    @PostMapping("/user/add")
    public WebResult<Void> addUser(@Validated @RequestBody UserForm userForm, @RequestHeader("Authorization") String authorization) {
        checkSuperAdmin(authorization);
        authService.createUser(
                userForm.getUsername(),
                userForm.getPassword(),
                userForm.getNickname(),
                userForm.getEmail(),
                userForm.getPhone(),
                userForm.getRole(),
                userForm.getStatus()
        );

        return WebResult.success();
    }

    @PostMapping("/user/update")
    public WebResult<Void> updateUser(@RequestBody UserForm userForm, @RequestHeader("Authorization") String authorization) {
        checkSuperAdmin(authorization);
        authService.updateUser(
                userForm.getUserId(),
                userForm.getNickname(),
                userForm.getEmail(),
                userForm.getPhone(),
                userForm.getRole(),
                userForm.getStatus()
        );

        return WebResult.success();
    }

    @PostMapping("/user/delete")
    public WebResult<Void> deleteUser(@RequestBody Map<String, Object> body, @RequestHeader("Authorization") String authorization) {
        checkSuperAdmin(authorization);
        String userId = (String) body.get("userId");
        authService.deleteUser(userId);

        return WebResult.success();
    }

    @PostMapping("/user/status")
    public WebResult<Void> updateUserStatus(@RequestBody Map<String, Object> body, @RequestHeader("Authorization") String authorization) {
        checkSuperAdmin(authorization);
        String userId = (String) body.get("userId");
        Integer status = (Integer) body.get("status");
        authService.updateUserStatus(userId, status);

        return WebResult.success();
    }

    private void checkSuperAdmin(String authorization) {
        String token = authorization.replace("Bearer ", "");
        Integer role = jwtTokenProvider.getRoleFromToken(token);
        if (role != 0) {
            throw new RuntimeException("无权限操作，仅超级管理员可访问");
        }
    }
}
