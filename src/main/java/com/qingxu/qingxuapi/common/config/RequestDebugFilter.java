package com.qingxu.qingxuapi.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestDebugFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        System.out.println("\n🔍 [RequestDebugFilter] ========== 请求开始 ==========");
        System.out.println("   Method: " + method);
        System.out.println("   URI: " + uri);
        System.out.println("   QueryString: " + (request.getQueryString() != null ? request.getQueryString() : "(无)"));
        System.out.println("   RemoteAddr: " + request.getRemoteAddr());
        System.out.println("   Requested Session ID: " + request.getRequestedSessionId());

        System.out.println("   📦 Headers:");
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            String value = request.getHeader(name);
            if (name.toLowerCase().contains("cookie") || name.toLowerCase().contains("authorization")) {
                if (name.toLowerCase().contains("cookie")) {
                    System.out.println("      " + name + ": [Cookie存在，长度=" + value.length() + "]");
                    if (value.contains("QINGXU_SESSION") || value.contains("SESSION")) {
                        System.out.println("      ✅ 发现 SESSION Cookie!");
                    }
                } else {
                    System.out.println("      " + name + ": [隐藏]");
                }
            } else {
                System.out.println("      " + name + ": " + value);
            }
        }

        System.out.println("   🍪 Cookies 详情:");
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                boolean isSessionCookie = cookie.getName().contains("SESSION");
                System.out.println("      * " + cookie.getName() +
                        " = " + (isSessionCookie ? "[值长度=" + cookie.getValue().length() + "]" : cookie.getValue()));
                System.out.println("        Domain: " + cookie.getDomain());
                System.out.println("        Path: " + cookie.getPath());
                System.out.println("        MaxAge: " + cookie.getMaxAge());
                System.out.println("        HttpOnly: " + cookie.isHttpOnly());
                System.out.println("        Secure: " + cookie.getSecure());
                if (isSessionCookie) {
                    System.out.println("        ⚠️  这是 Session Cookie!");
                }
            }
        } else {
            System.out.println("      ⚠️  (无 Cookie!)");
            System.out.println("      ❌ 如果这是需要认证的请求，这就是401的原因!");
        }

        System.out.println("   🔐 SecurityContext (请求前): " +
                (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null ?
                        SecurityContextHolder.getContext().getAuthentication().getName() +
                                " [" + SecurityContextHolder.getContext().getAuthentication().getAuthorities() + "]" :
                        "(未认证)"));

        long startTime = System.currentTimeMillis();

        filterChain.doFilter(request, response);

        long duration = System.currentTimeMillis() - startTime;

        System.out.println("   📊 响应状态: " + response.getStatus());
        System.out.println("   ⏱️  处理耗时: " + duration + "ms");
        System.out.println("   🔐 SecurityContext (请求后): " +
                (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null ?
                        SecurityContextHolder.getContext().getAuthentication().getName() :
                        "(未认证)"));
        System.out.println("🔍 [RequestDebugFilter] ========== 请求结束 ==========\n");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/actuator") ||
               path.contains("/swagger-ui") ||
               path.contains("/v3/api-docs") ||
               path.contains(".css") ||
               path.contains(".js") ||
               path.contains(".ico") ||
               path.contains(".png") ||
               path.contains(".woff");
    }
}