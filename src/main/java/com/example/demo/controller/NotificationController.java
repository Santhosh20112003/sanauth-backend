package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.RedisService;

@RestController
@RequestMapping("/redis")
public class NotificationController {
	@Autowired
	private RedisService redisService;

	@PostMapping("/save")
	public String save(@RequestParam String key, @RequestParam Object value) {
		redisService.save(key, value);
		return "Saved!";
	}

	@GetMapping("/get")
	public Object get(@RequestParam String key) {
		return redisService.get(key);
	}
	
	@DeleteMapping("/delete")
	public String delete(@RequestParam String key) {
		redisService.delete(key);
		return "Deleted!";
	}
	
	@GetMapping("/all/logins")
	public Object getAllKeys(@RequestParam String email) {
		return redisService.getAllKeys(email);
	}
	
}
