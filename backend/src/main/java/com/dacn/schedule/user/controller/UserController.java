package com.dacn.schedule.user.controller;

import com.dacn.schedule.common.api.ApiPaths;
import com.dacn.schedule.common.exception.ApiException;
import com.dacn.schedule.common.exception.ErrorCode;
import com.dacn.schedule.common.security.AuthenticatedUser;
import com.dacn.schedule.user.dto.UserResponse;
import com.dacn.schedule.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/users")
@Tag(name = "Người dùng", description = "Thông tin tài khoản đang đăng nhập")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Thông tin tài khoản hiện tại", description = "Cần access token.")
    public UserResponse me(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser currentUser) {
        // Token còn hạn nhưng tài khoản đã bị xóa: coi như chưa đăng nhập
        return userService.findById(currentUser.userId())
                .map(user -> UserResponse.from(user, currentUser.roles()))
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
    }
}
