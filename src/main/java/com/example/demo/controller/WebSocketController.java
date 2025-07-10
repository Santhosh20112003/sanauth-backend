package com.example.demo.controller;

import com.example.demo.modal.WebSocketPayload;
import com.example.demo.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class WebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);

    @Autowired
    private RedisService redisService;

    @MessageMapping("/publish/{email}")
    @SendTo("/topic/{email}")
    public WebSocketPayload publishMessage(WebSocketPayload payload, @DestinationVariable String email) {
        logger.info("Publishing message for [{}]: {}", email, payload);

        String key = payload.getEmail() + "_" + payload.getToken();

        if (redisService.keyExists(key)) {
            logger.warn("Key already exists in Redis: {}", key);
            // ❌ Do NOT broadcast → return null so Spring ignores
            return null;
        }

        try {
            redisService.save(key, payload);
            logger.info("Payload saved to Redis with key: {}", key);
        } catch (Exception e) {
            logger.error("Error saving payload to Redis: {}", e.getMessage(), e);
            // Optionally send error payload (or null if you don’t want to broadcast)
            return null;
        }

        logger.info("Broadcasting message to /topic/{}", email);
        return new WebSocketPayload(
                payload.getEmail(),
                payload.getType(),
                payload.getToken(),
                "Broadcasted message: " + payload.getMessage()
        );
    }

    @GetMapping("/subscribe/{email}")
    public WebSocketPayload subscribeMessage(@PathVariable String email) {
        logger.info("Fetching message for email: {}", email);

        String keyPattern = email + "_*";
        WebSocketPayload payload = redisService.getByPattern(keyPattern);

        if (payload == null) {
            logger.warn("No matching key found for email: {}", email);
            return new WebSocketPayload(email, null, null, "No data found for the given email.");
        }

        logger.info("Returning payload for email: {}", email);
        return payload;
    }
}
