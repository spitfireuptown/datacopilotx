package com.datacopilotx.ai.service;

import com.datacopilotx.ai.domian.vo.UserInfoVo;

import java.util.Map;

public interface AuthService {

    String login(String username, String password);

    void register(String username, String password, String nickname, String email, String phone);

    UserInfoVo getCurrentUserInfo(String userId);

    void changePassword(String userId, String oldPassword, String newPassword);

    void resetPasswordByUsername(String username, String newPassword);

    Map<String, Object> getUserList(String username, Integer page, Integer size);

    UserInfoVo getUserById(String userId);

    void createUser(String username, String password, String nickname, String email, String phone, Integer role, Integer status);

    void updateUser(String userId, String nickname, String email, String phone, Integer role, Integer status);

    void deleteUser(String userId);

    void updateUserStatus(String userId, Integer status);

    void resetPassword(String userId);
}