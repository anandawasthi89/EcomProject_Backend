package com.project.ecomapp.ecommerce_Project.publisher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisListPublisher {
    @Autowired
    RedisTemplate<String, BookUploadPayload> redisTemplate;

    public void publish(BookUploadPayload bookUploadPayload){
        redisTemplate.opsForList().leftPush(bookUploadPayload.getEventId(),bookUploadPayload);
    }

    public BookUploadPayload dummyConsume(String key){
        BookUploadPayload bookUploadPayload = redisTemplate.opsForList().rightPop(key);
        assert bookUploadPayload != null;
        System.out.println(bookUploadPayload.toString());
        return bookUploadPayload;
    }
}
