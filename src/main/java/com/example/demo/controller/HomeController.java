package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/home")
    public String home() {
        return "home"; 
    }
    
    @GetMapping("/client1")
    public String client1() {
        return "client1"; 
    }
    
    @GetMapping("/client2")
    public String client2() {
        return "client2"; 
    }
    
    @GetMapping("/admin")
    public String admin() {
        return "admin"; 
    }
    
}