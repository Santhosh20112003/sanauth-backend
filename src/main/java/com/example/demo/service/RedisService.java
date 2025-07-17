package com.example.demo.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.modal.WebSocketPayload;

@Service
public class RedisService {

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	public Object save(String key, Object value) {
		try {
			redisTemplate.opsForValue().set(key, value);
			return value;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public Object get(String key) {
		try {
			return redisTemplate.opsForValue().get(key);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void delete(String key) {
		try {
			redisTemplate.delete(key);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<Object> getAllKeys(String email) {
		String pattern = email + ":*";
		try {
			Set<String> keys = redisTemplate.keys(pattern);
			if (keys == null || keys.isEmpty()) {
				return Collections.emptyList();
			}

			List<Object> values = keys.stream().map(key -> redisTemplate.opsForValue().get(key))
					.filter(Objects::nonNull).toList();

			return values;
		} catch (Exception e) {
			e.printStackTrace(); // optionally use a logger instead
			return Collections.emptyList();
		}
	}

	/**
	 * Checks if a key exists in Redis.
	 * 
	 * @param key the key to check
	 * @return true if the key exists, false otherwise
	 */
	public boolean keyExists(String key) {
		try {
			return redisTemplate.hasKey(key);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public WebSocketPayload getByPattern(String keyPattern) {
		try {
			Set<String> keys = redisTemplate.keys(keyPattern);
			if (keys == null || keys.isEmpty()) {
				return null; // No matching keys found
			}

			List<Object> values = new ArrayList<>();
			for (String key : keys) {
				Object value = redisTemplate.opsForValue().get(key);
				if (value instanceof WebSocketPayload) {
					values.add(value);
				}
			}

			if (!values.isEmpty()) {
				return (WebSocketPayload) values.get(0); // Return the first matching payload
			} else {
				return null; // No valid WebSocketPayload found
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null; // Handle exceptions appropriately
		}
	}

	public void deleteByPattern(String usedOtpKeyPattern) {
		try {
			Set<String> keys = redisTemplate.keys(usedOtpKeyPattern);
			if (keys != null && !keys.isEmpty()) {
				redisTemplate.delete(keys);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
