package com.codepliot.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

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
    public String forwardToFrontend() {
        return "forward:/index.html";
    }
}
