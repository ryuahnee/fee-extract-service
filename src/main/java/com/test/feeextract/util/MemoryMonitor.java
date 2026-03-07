package com.test.feeextract.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class MemoryMonitor {
    
    private final DecimalFormat df = new DecimalFormat("#,###.##");
    private final Map<String, Long> memorySnapshots = new ConcurrentHashMap<>();
    
    /**
     * 현재 메모리 사용량 측정 및 로깅
     */
    public MemoryInfo measureMemory(String phase) {
        Runtime runtime = Runtime.getRuntime();
        
        // 강제 GC 후 측정 (정확한 측정을 위해)
        System.gc();
        
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        MemoryInfo memoryInfo = MemoryInfo.builder()
                .phase(phase)
                .usedMemoryMB(usedMemory / 1024 / 1024)
                .maxMemoryMB(maxMemory / 1024 / 1024)
                .totalMemoryMB(totalMemory / 1024 / 1024)
                .freeMemoryMB(freeMemory / 1024 / 1024)
                .usagePercent((double) usedMemory / maxMemory * 100)
                .build();
        
        // 이전 측정값과 비교
        String prevKey = "prev_" + phase;
        Long prevMemory = memorySnapshots.get(prevKey);
        long memoryDiff = prevMemory != null ? (usedMemory - prevMemory) : 0;
        
        log.info("📊 메모리 상태 [{}]", phase);
        log.info("   사용 메모리: {}MB / {}MB ({}%)", 
                df.format(memoryInfo.getUsedMemoryMB()),
                df.format(memoryInfo.getMaxMemoryMB()),
                df.format(memoryInfo.getUsagePercent()));
        
        if (memoryDiff != 0) {
            log.info("   메모리 변화: {}MB", df.format(memoryDiff / 1024 / 1024));
        }
        
        // 현재 값 저장
        memorySnapshots.put(prevKey, usedMemory);
        
        return memoryInfo;
    }
    
    /**
     * 메모리 사용량 차이 계산
     */
    public long calculateMemoryDiff(MemoryInfo before, MemoryInfo after) {
        return after.getUsedMemoryMB() - before.getUsedMemoryMB();
    }
    
    /**
     * 메모리 정보 DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class MemoryInfo {
        private String phase;
        private long usedMemoryMB;
        private long maxMemoryMB;
        private long totalMemoryMB;
        private long freeMemoryMB;
        private double usagePercent;
    }
}
