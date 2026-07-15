package com.bridgeinspection.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "登录账号不能为空")
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{3,29}$", message = "账号须以字母开头，只能包含字母、数字和下划线，长度4至30位")
        String account,
        @NotBlank(message = "真实姓名不能为空")
        @Size(max = 50, message = "真实姓名不能超过50个字符")
        String realName,
        @NotBlank(message = "密码不能为空")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$", message = "密码须为8至64位并同时包含字母和数字")
        String password,
        @NotBlank(message = "确认密码不能为空")
        String confirmPassword,
        @Size(max = 100, message = "单位部门不能超过100个字符")
        String department,
        @Size(max = 50, message = "联系电话不能超过50个字符")
        String phone,
        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱不能超过100个字符")
        String email
) {}
