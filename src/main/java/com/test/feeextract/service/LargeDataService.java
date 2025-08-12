package com.test.feeextract.service;

import com.test.feeextract.domain.JobInfo;
import com.test.feeextract.domain.JobStatus;
import com.test.feeextract.util.MemoryMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class LargeDataService {
    
    private final MemoryMonitor memoryMonitor;
    private final SimpleAsyncService asyncService;
    
    /**
     * 50만건 데이터 일반 처리 (전체 메모리 로딩)
     */
    @Async
    public CompletableFuture<String> processLargeData(String jobId, int dataSize) {
        try {
            log.info("🚀 50만건 데이터 처리 시작 - 크기: {}", dataSize);
            
            // 시작 전 메모리 측정
            MemoryMonitor.MemoryInfo startMemory = memoryMonitor.measureMemory("작업 시작 전");
            
            // 1단계: 데이터 생성 (메모리에 모든 데이터 로딩)
            asyncService.updateJob(jobId, JobStatus.RUNNING, 10, "대용량 데이터 생성 중...");
            List<UserData> dataList = generateLargeDataset(dataSize);
            
            MemoryMonitor.MemoryInfo afterGeneration = memoryMonitor.measureMemory("데이터 생성 후");
            long generationMemory = memoryMonitor.calculateMemoryDiff(startMemory, afterGeneration);
            
            // 2단계: 데이터 처리
            asyncService.updateJob(jobId, JobStatus.RUNNING, 50, "데이터 처리 중...");
            String result = processDataList(dataList);
            
            MemoryMonitor.MemoryInfo afterProcessing = memoryMonitor.measureMemory("데이터 처리 후");
            long processingMemory = memoryMonitor.calculateMemoryDiff(afterGeneration, afterProcessing);
            
            // 3단계: 결과 정리
            dataList.clear(); // 명시적으로 메모리 해제
            dataList = null;
            
            MemoryMonitor.MemoryInfo afterCleanup = memoryMonitor.measureMemory("메모리 정리 후");
            
            // 결과 리포트
            String memoryReport = String.format(
                "📊 메모리 사용량 리포트\n" +
                "- 데이터 생성: %dMB\n" +
                "- 데이터 처리: %dMB\n" +
                "- 최대 사용량: %dMB\n" +
                "- 정리 후: %dMB",
                generationMemory,
                processingMemory,
                afterProcessing.getUsedMemoryMB(),
                afterCleanup.getUsedMemoryMB()
            );
            
            log.info(memoryReport);
            asyncService.completeJob(jobId, "50만건 데이터 처리 완료\n" + memoryReport);
            
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            log.error("대용량 데이터 처리 중 오류", e);
            asyncService.failJob(jobId, "오류 발생: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * 50만건 데이터 스트리밍 처리 (메모리 효율적)
     */
    @Async
    public CompletableFuture<String> processLargeDataStreaming(String jobId, int dataSize) {
        try {
            log.info("🚀 50만건 데이터 스트리밍 처리 시작 - 크기: {}", dataSize);
            
            MemoryMonitor.MemoryInfo startMemory = memoryMonitor.measureMemory("스트리밍 시작 전");
            
            // 배치 단위로 처리 (메모리 절약)
            int batchSize = 1000;
            int processedCount = 0;
            
            for (int i = 0; i < dataSize; i += batchSize) {
                int currentBatchSize = Math.min(batchSize, dataSize - i);
                
                // 소량의 데이터만 메모리에 로딩
                List<UserData> batch = generateBatchData(i, currentBatchSize);
                
                // 배치 처리
                processBatch(batch);
                
                // 즉시 메모리 해제
                batch.clear();
                batch = null;
                
                processedCount += currentBatchSize;
                int progress = (processedCount * 100) / dataSize;
                
                // 진행률 업데이트
                if (processedCount % 10000 == 0) {
                    asyncService.updateJob(jobId, JobStatus.RUNNING, progress, 
                        String.format("스트리밍 처리 중... (%,d/%,d)", processedCount, dataSize));
                    
                    // 주기적으로 메모리 상태 확인
                    memoryMonitor.measureMemory("배치 처리 중 (진행률: " + progress + "%)");
                }
                
                // 메모리 정리 힌트
                if (processedCount % 50000 == 0) {
                    System.gc();
                }
            }
            
            MemoryMonitor.MemoryInfo endMemory = memoryMonitor.measureMemory("스트리밍 처리 완료");
            long totalMemoryUsed = memoryMonitor.calculateMemoryDiff(startMemory, endMemory);
            
            String result = String.format("스트리밍 처리 완료 - 총 메모리 사용량: %dMB", totalMemoryUsed);
            asyncService.completeJob(jobId, result);
            
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            log.error("스트리밍 처리 중 오류", e);
            asyncService.failJob(jobId, "오류 발생: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * 대용량 데이터셋 생성 (메모리에 모든 데이터 로딩)
     */
    private List<UserData> generateLargeDataset(int size) {
        log.info("📝 {}건 데이터 생성 시작", size);
        
        List<UserData> dataList = new ArrayList<>(size);
        
        for (int i = 0; i < size; i++) {
            dataList.add(UserData.builder()
                .id(i + 1)
                .name("User_" + (i + 1))
                .email("user" + (i + 1) + "@test.com")
                .description("This is user number " + (i + 1) + " with some additional data for memory testing purposes.")
                .createdAt(LocalDateTime.now())
                .build());
            
            // 진행률 로깅
            if ((i + 1) % 100000 == 0) {
                log.info("데이터 생성 진행률: {}/{} ({}%)", 
                    i + 1, size, ((i + 1) * 100) / size);
            }
        }
        
        log.info("✅ {}건 데이터 생성 완료", size);
        return dataList;
    }
    
    /**
     * 배치 데이터 생성 (소량씩 생성)
     */
    private List<UserData> generateBatchData(int startIndex, int batchSize) {
        List<UserData> batch = new ArrayList<>(batchSize);
        
        for (int i = 0; i < batchSize; i++) {
            int id = startIndex + i + 1;
            batch.add(UserData.builder()
                .id(id)
                .name("User_" + id)
                .email("user" + id + "@test.com")
                .description("This is user number " + id + " with some additional data for memory testing purposes.")
                .createdAt(LocalDateTime.now())
                .build());
        }
        
        return batch;
    }
    
    /**
     * 데이터 리스트 처리
     */
    private String processDataList(List<UserData> dataList) {
        log.info("📊 데이터 처리 시작 - 크기: {}", dataList.size());
        
        // 시뮬레이션: 각 데이터에 대해 간단한 처리
        long totalProcessingTime = 0;
        
        for (UserData data : dataList) {
            long startTime = System.nanoTime();
            
            // 실제 처리 시뮬레이션
            String processed = data.getName().toUpperCase() + "_PROCESSED";
            data.setProcessedName(processed);
            
            long endTime = System.nanoTime();
            totalProcessingTime += (endTime - startTime);
        }
        
        log.info("✅ 데이터 처리 완료 - 총 처리 시간: {}ms", 
            totalProcessingTime / 1_000_000);
        
        return "Processing completed for " + dataList.size() + " records";
    }
    
    /**
     * 배치 처리
     */
    private void processBatch(List<UserData> batch) {
        for (UserData data : batch) {
            // 간단한 데이터 처리
            String processed = data.getName().toUpperCase() + "_PROCESSED";
            data.setProcessedName(processed);
        }
    }
    
    /**
     * 테스트용 사용자 데이터 클래스
     */
    @lombok.Data
    @lombok.Builder
    public static class UserData {
        private int id;
        private String name;
        private String email;
        private String description;
        private LocalDateTime createdAt;
        private String processedName;
        
        // 대략적인 메모리 사용량 계산 (문자열 기준)
        public int getApproximateMemorySize() {
            int size = 0;
            size += Integer.BYTES; // id
            size += (name != null ? name.length() * 2 : 0); // String은 char당 2바이트
            size += (email != null ? email.length() * 2 : 0);
            size += (description != null ? description.length() * 2 : 0);
            size += (processedName != null ? processedName.length() * 2 : 0);
            size += 24; // LocalDateTime 대략적 크기
            return size;
        }
    }
}
