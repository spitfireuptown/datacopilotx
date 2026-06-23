package com.datacopilotx.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacopilotx.ai.config.security.JwtTokenProvider;
import com.datacopilotx.ai.domian.bean.UserBean;
import com.datacopilotx.ai.domian.vo.UserInfoVo;
import com.datacopilotx.ai.mapper.UserMapper;
import com.datacopilotx.ai.service.AuthService;
import com.datacopilotx.common.exception.DataCopilotXException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.datacopilotx.common.result.ResponseCode.AUTH_USER_ERROR;
import static com.datacopilotx.common.result.ResponseCode.AUTH_USER_NOT_FOUND_ERROR;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public String login(String username, String password) {
        UserBean user = userMapper.findByUsername(username);
        if (user == null) {
            throw new DataCopilotXException("用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new DataCopilotXException("账号已被禁用");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new DataCopilotXException(AUTH_USER_ERROR);
        }

        return jwtTokenProvider.generateToken(user.getUserId(), user.getUsername(), user.getRole());
    }

    @Override
    public void register(String username, String password, String nickname, String email, String phone) {
        if (userMapper.exists(new LambdaQueryWrapper<UserBean>()
                .eq(UserBean::getUsername, username)
                .eq(UserBean::getIsDel, 0))) {
            throw new DataCopilotXException("用户名已存在");
        }

        UserBean user = UserBean.builder()
                .userId(UUID.randomUUID().toString())
                .username(username)
                .password(passwordEncoder.encode(password))
                .nickname(nickname)
                .email(email)
                .phone(phone)
                .role(2)
                .status(1)
                .isDel(0)
                .ctime(new Timestamp(System.currentTimeMillis()))
                .utime(new Timestamp(System.currentTimeMillis()))
                .build();

        userMapper.insert(user);
    }

    @Override
    public UserInfoVo getCurrentUserInfo(String userId) {
        UserBean user = userMapper.findByUserId(userId);
        if (user == null) {
            throw new DataCopilotXException(AUTH_USER_NOT_FOUND_ERROR);
        }

        return convertToVo(user);
    }

    @Override
    public void changePassword(String userId, String oldPassword, String newPassword) {
        UserBean user = userMapper.findByUserId(userId);
        if (user == null) {
            throw new DataCopilotXException(AUTH_USER_NOT_FOUND_ERROR);
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new DataCopilotXException("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUtime(new Timestamp(System.currentTimeMillis()));
        userMapper.updateById(user);
    }

    @Override
    public void resetPasswordByUsername(String username, String newPassword) {
        UserBean user = userMapper.findByUsername(username);
        if (user == null) {
            throw new DataCopilotXException(AUTH_USER_NOT_FOUND_ERROR);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUtime(new Timestamp(System.currentTimeMillis()));
        userMapper.updateById(user);
    }

    @Override
    public Map<String, Object> getUserList(String username, Integer page, Integer size) {
        Page<UserBean> userPage = new Page<>(page, size);

        LambdaQueryWrapper<UserBean> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(UserBean::getUsername, "admin");
        queryWrapper.eq(UserBean::getIsDel, 0);

        if (StringUtils.hasText(username)) {
            queryWrapper.and(w -> w.like(UserBean::getUsername, username)
                    .or().like(UserBean::getNickname, username));
        }

        queryWrapper.orderByDesc(UserBean::getCtime);
        userMapper.selectPage(userPage, queryWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("list", userPage.getRecords().stream()
                .map(this::convertToVo)
                .collect(Collectors.toList()));
        result.put("total", userPage.getTotal());

        return result;
    }

    @Override
    public UserInfoVo getUserById(String userId) {
        UserBean user = userMapper.findByUserId(userId);
        if (user == null) {
            throw new DataCopilotXException(AUTH_USER_NOT_FOUND_ERROR);
        }
        return convertToVo(user);
    }

    @Override
    public void createUser(String username, String password, String nickname, String email, String phone, Integer role, Integer status) {
        if (userMapper.exists(new LambdaQueryWrapper<UserBean>()
                .eq(UserBean::getUsername, username)
                .eq(UserBean::getIsDel, 0))) {
            throw new DataCopilotXException("用户名已存在");
        }

        UserBean user = UserBean.builder()
                .userId(UUID.randomUUID().toString())
                .username(username)
                .password(passwordEncoder.encode(password))
                .nickname(nickname)
                .email(email)
                .phone(phone)
                .role(role)
                .status(status)
                .isDel(0)
                .ctime(new Timestamp(System.currentTimeMillis()))
                .utime(new Timestamp(System.currentTimeMillis()))
                .build();

        userMapper.insert(user);
    }

    @Override
    public void updateUser(String userId, String nickname, String email, String phone, Integer role, Integer status) {
        UserBean user = userMapper.findByUserId(userId);
        if (user == null) {
            throw new DataCopilotXException(AUTH_USER_NOT_FOUND_ERROR);
        }

        if (nickname != null) {
            user.setNickname(nickname);
        }
        if (email != null) {
            user.setEmail(email);
        }
        if (phone != null) {
            user.setPhone(phone);
        }
        if (role != null) {
            user.setRole(role);
        }
        if (status != null) {
            user.setStatus(status);
        }

        user.setUtime(new Timestamp(System.currentTimeMillis()));
        userMapper.updateById(user);
    }

    @Override
    public void deleteUser(String userId) {
        UserBean user = userMapper.findByUserId(userId);
        if (user == null) {
            throw new DataCopilotXException(AUTH_USER_NOT_FOUND_ERROR);
        }
        user.setIsDel(1);
        user.setUtime(new Timestamp(System.currentTimeMillis()));
        userMapper.updateById(user);
    }

    @Override
    public void updateUserStatus(String userId, Integer status) {
        UserBean user = userMapper.findByUserId(userId);
        if (user == null) {
            throw new DataCopilotXException(AUTH_USER_NOT_FOUND_ERROR);
        }
        user.setStatus(status);
        user.setUtime(new Timestamp(System.currentTimeMillis()));
        userMapper.updateById(user);
    }

    @Override
    public void resetPassword(String userId) {
        UserBean user = userMapper.findByUserId(userId);
        if (user == null) {
            throw new DataCopilotXException(AUTH_USER_NOT_FOUND_ERROR);
        }
        user.setPassword(passwordEncoder.encode("datacopilotx"));
        user.setUtime(new Timestamp(System.currentTimeMillis()));
        userMapper.updateById(user);
    }

    private UserInfoVo convertToVo(UserBean user) {
        return UserInfoVo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .roleDesc(UserBean.Role.fromCode(user.getRole()).getDesc())
                .status(user.getStatus())
                .statusDesc(UserBean.Status.fromCode(user.getStatus()).getDesc())
                .createdAt(user.getCtime() != null ? user.getCtime().getTime() : null)
                .updatedAt(user.getUtime() != null ? user.getUtime().getTime() : null)
                .build();
    }
}