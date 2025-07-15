package com.test.feeextract.controller;


import com.test.feeextract.service.SimpleAsyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/async")
public class AsyncController {

    private final SimpleAsyncService asyncService;

    public AsyncController(SimpleAsyncService asyncService) {
        this.asyncService = asyncService;
    }

//async/single - 단일 작업 API
//async/batch - 배치 작업 API
//async/compare - 비교 테스트 API
    

    @GetMapping("/single")
    public String processData() {
        // 각 API 시작할 때
        log.info("API 호출: [{}] ", "processData");
        asyncService.processData();
        String success = "success";
        // 각 API 끝날 때
        log.info("API 응답: [{}] - 결과: {}", "processData", success);
        return success;
    }

    @GetMapping("/user/{name}")
    public String processUser(@PathVariable String name) {
        log.info("API 호출: [processUser] - 사용자: {}", name);
        asyncService.processUser(name);
        String response = "✅ " + name + " 비동기 처리 시작됨!";
        log.info("API 응답: [processUser] - 결과: {}", response);
        return response;
    }

    @PostMapping("/batch")
    public String  processBatch(@RequestBody List<String> userNames) {
        log.info("API 호출: [processUser] - 사용자: {}", userNames);
        asyncService.processBatch(userNames);
        String response = "✅ " + userNames + " 비동기 처리 시작됨!";
        log.info("API 응답: [processUser] - 결과: {}", response);
        return response;
    }
    
    
    
}
