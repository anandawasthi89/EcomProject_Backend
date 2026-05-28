package com.project.ecomapp.ecommerce_Project.publisher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisListPublisher {
    @Autowired
    RedisTemplate<String, BookUploadPayload> redisTemplate;
    private final String QUEUE_KEY = "Book_Upload_Queue";

    public void publish(BookUploadPayload bookUploadPayload){
        redisTemplate.opsForList().leftPush(QUEUE_KEY, bookUploadPayload);
    }

    public BookUploadPayload dummyConsume(){
        BookUploadPayload bookUploadPayload = redisTemplate.opsForList().rightPop(QUEUE_KEY);
        assert bookUploadPayload != null;
        System.out.println(bookUploadPayload.toString());
        return bookUploadPayload;
    }
}
