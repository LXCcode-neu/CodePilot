package com.codepliot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
/**
 * FrontendRouteController 控制器，负责对外提供 HTTP 接口。
 */
@Controller
public class FrontendRouteController {
@GetMapping({
            "/login",
            "/register",
            "/projects",
            "/tasks",
            "/tasks/new",
            "/tasks/{id:[0-9]+}"
    })
/**
 * 执行 forwardToFrontend 相关逻辑。
 */
public String forwardToFrontend() {
        return "forward:/index.html";
    }
}

