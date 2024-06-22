package com.ecommerce.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.Service.MailService;
import com.ecommerce.Service.UserService;

@RestController
@RequestMapping("/mail")
public class MailController {
	
    @Autowired 
    private UserService userService;
	
    @GetMapping("/confirm")
    public String confirmUser(@RequestParam("token") String token) {
        userService.confirmUser(token);
        return "Tài khoản của bạn đã được kích hoạt.";
    }
}

