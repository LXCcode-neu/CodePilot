package com.codepliot.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 前端路由转发控制器。
 * 在 Spring Boot 托管前端静态资源时，将单页应用路由统一转发到 index.html。
 */
@Controller
public class FrontendRouteController {

    /**
     * 将前端页面路由转发到单页应用入口。
     */
    @GetMapping({
            "/login",
            "/register",
            "/projects",
            "/tasks",
            "/tasks/new",
            "/tasks/{id:[0-9]+}"
    })
    public String forwardToFrontend() {
        return "forward:/index.html";
    }
}
