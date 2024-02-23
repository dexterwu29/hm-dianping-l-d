package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import java.time.LocalDateTime;

import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2.不符合要求，抛出异常
            return Result.fail("手机号格式无效");
        }
        // 3.符合要求，生成验证码
        String genCode = RandomUtil.randomNumbers(6);
        // 4.保存验证码到session
        session.setAttribute(phone, genCode);
        // 5.发送验证码 TODO 后面可以改用云短信服务或者用邮箱实现
        log.debug("发送短信验证码成功，验证码为：{}", genCode);
        // 返回ok
        return Result.ok();
    }

    @Override
    public Result userLogin(LoginFormDTO loginForm, HttpSession session) {
        // 1.校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2.不符合要求，抛出异常
            return Result.fail("手机号格式无效");
        }
        // 3.校验验证码
        Object cacheCode = session.getAttribute(phone);
        if (cacheCode == null || !cacheCode.equals(loginForm.getCode())) {
            return Result.fail("验证码错误");
        }
        // 4.校验成功，查询数据库用户
        User user = query().eq("phone", phone).one();
        if (user == null) {
            // 5.查询不到用户，创建新用户【注册】
            user = userRegister(phone);
        }
        // 5.保存用户登录信息【脱敏】
        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
        return Result.ok();
    }

    /**
     * 创建新用户【注册】
     *
     * @param phone
     * @return
     */
    private User userRegister(String phone) {
        // 1.创建新用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        // 2.数据插入数据库
        save(user);
        return user;
    }
}
