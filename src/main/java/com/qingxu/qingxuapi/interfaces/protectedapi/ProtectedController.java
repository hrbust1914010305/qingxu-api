package com.qingxu.qingxuapi.interfaces.protectedapi;

import com.qingxu.qingxuapi.common.response.ApiResponse;
import com.qingxu.qingxuapi.common.response.ResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/protected")
@RequiredArgsConstructor
public class ProtectedController {

    private final ResponseFactory responseFactory;

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return responseFactory.success("pong");
    }
}
