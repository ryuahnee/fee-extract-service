package com.test.feeextract.controller;

import com.test.feeextract.domain.JobInfo;
import com.test.feeextract.service.LargeDataService;
import com.test.feeextract.service.SimpleAsyncService;
import com.test.feeextract.util.MemoryMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/memory-test")
@RequiredArgsConstructor
@Slf4j
public class MemoryTestController {
    
    private final LargeDataService largeDataService;
    private final SimpleAsyncService asyncService;
    private final MemoryMonitor memoryMonitor;
    
    /**
     * 50만건 데이터 일반 처리 (전체 메모리 로딩)
     */
    @PostMapping("/large-data")
    public String testLargeData(@RequestParam(defaultValue = "500000") int dataSize) {
        log.info("🚀 대용량 데이터 테스트 시작 - 크기: {}", dataSize);
        
        String jobId = asyncService.createJob("LargeDataProcessing");
        largeDataService.processLargeData(jobId, dataSize);
        
        return String.format("✅ 대용량 데이터 처리 시작됨! (작업 ID: %s, 크기: %,d건)", jobId, dataSize);
    }
    
    /**
     * 50만건 데이터 스트리밍 처리 (메모리 효율적)
     */
    @PostMapping("/large-data-streaming")
    public String testLargeDataStreaming(@RequestParam(defaultValue = "500000") int dataSize) {
        log.info("🚀 대용량 데이터 스트리밍 테스트 시작 - 크기: {}", dataSize);
        
        String jobId = asyncService.createJob("LargeDataStreamingProcessing");
        largeDataService.processLargeDataStreaming(jobId, dataSize);
        
        return String.format("✅ 스트리밍 데이터 처리 시작됨! (작업 ID: %s, 크기: %,d건)", jobId, dataSize);
    }
    
    /**
     * 작업 상태 조회
     */
    @GetMapping("/job/{jobId}")
    public JobInfo getJobStatus(@PathVariable String jobId) {
        return asyncService.getJob(jobId);
    }
    
    /**
     * 현재 메모리 상태 확인
     */
    @GetMapping("/memory-status")
    public MemoryMonitor.MemoryInfo getCurrentMemoryStatus() {
        return memoryMonitor.measureMemory("현재 상태");
    }
    
    /**
     * 메모리 정리 강제 실행
     */
    @PostMapping("/gc")
    public String forceGarbageCollection() {
        MemoryMonitor.MemoryInfo before = memoryMonitor.measureMemory("GC 실행 전");
        
        System.gc();
        System.runFinalization();
        
        // 잠시 대기
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        MemoryMonitor.MemoryInfo after = memoryMonitor.measureMemory("GC 실행 후");
        long memoryFreed = memoryMonitor.calculateMemoryDiff(after, before); // 음수가 나올 것임
        
        return String.format("🧹 가비지 컬렉션 완료! 해제된 메모리: %dMB", Math.abs(memoryFreed));
    }
    
    /**
     * 다양한 크기별 메모리 테스트
     */
    @PostMapping("/memory-comparison")
    public String testMemoryComparison() {
        log.info("📊 메모리 사용량 비교 테스트 시작");
        
        // 여러 크기로 테스트
        int[] testSizes = {10000, 50000, 100000, 500000};
        
        for (int size : testSizes) {
            String jobId = asyncService.createJob("MemoryComparison_" + size);
            largeDataService.processLargeData(jobId, size);
            
            // 각 테스트 간 잠시 대기
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        return "📊 메모리 사용량 비교 테스트 시작됨! 크기별로 순차 실행됩니다.";
    }
}
