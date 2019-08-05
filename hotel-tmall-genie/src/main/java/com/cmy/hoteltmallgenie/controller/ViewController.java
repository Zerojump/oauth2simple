package com.cmy.hoteltmallgenie.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * <p>@author chenmingyi
 * <p>@version 1.0
 * <p>date: 2019/8/5
 */
@Controller
public class ViewController {
    /**
     * login
     */
    @RequestMapping(value = "oauth2login")
    public String login() {
        return "jsp/oauth2login";
    }

    /**
     * index
     */
    @RequestMapping(value = "index")
    public String index() {
        return "index";
    }
}
