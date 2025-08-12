package com.test.feeextract.service;

import com.test.feeextract.domain.JobInfo;
import com.test.feeextract.domain.JobStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SimpleAsyncService {

    // 작업들을 저장할 map
    private final Map<String, JobInfo> jobs = new ConcurrentHashMap<>();

    // 잡 생성
    public String createJob(String taskName){
        String jobId = UUID.randomUUID().toString();

        jobs.put(jobId,
                JobInfo.builder()
                        .jobId(jobId)
                        .status(JobStatus.RUNNING)
                        .progress(0)
                        .message("작업준비중..")
                        .startTime(LocalDateTime.now())
                        .build());
        return jobId;
    }

    // 작업 상태 업데이트
    public void updateJob(String jobId , JobStatus status, int progress, String message){

            jobs.computeIfPresent(jobId,(key,existingJob)->{
                log.debug("작업 상태 업데이트 - ID: {}, 상태: {}, 진행률: {}%", jobId, status, progress);
                return JobInfo.builder()
                        .jobId(existingJob.getJobId())
                        .status(status)
                        .progress(progress)
                        .message(message)
                        .startTime(existingJob.getStartTime())
                        .endTime(LocalDateTime.now())
                        .build();
            });
    }

    // 작업 상태 조회
    public JobInfo getJob(String jobId) {
        return jobs.get(jobId);
    }

    // 작업 완료 처리
    public void completeJob(String jobId, String message) {
        updateJob(jobId, JobStatus.COMPLETED ,100, message);
    }

    // 작업 실패 처리
    public void failJob(String jobId, String errorMessage) {
        updateJob(jobId,JobStatus.FAILED ,0, errorMessage);
    }


    // 주기적으로 오래된 작업 정리 (메모리 누수 방지)
    @Scheduled(fixedRate = 300000) // 5분마다 실행
    public void cleanupOldJobs() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
        int beforeSize = jobs.size();
        
        jobs.entrySet().removeIf(entry -> {
            JobInfo job = entry.getValue();
            return job.getStartTime().isBefore(cutoff) && 
                   (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.FAILED);
        });
        
        int afterSize = jobs.size();
        if (beforeSize > afterSize) {
            log.info("🧹 오래된 작업 정리 완료 - 정리 전: {}개, 정리 후: {}개", beforeSize, afterSize);
        }
    }

    // 현재 메모리 사용량 로깅
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void logMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        double usagePercent = (double) usedMemory / maxMemory * 100;
        
        log.info("📊 메모리 상태 - 사용: {}MB/{}MB ({}%), 활성 작업: {}개", 
                usedMemory / 1024 / 1024,
                maxMemory / 1024 / 1024,
                String.format("%.2f", usagePercent),
                jobs.size());
    }


    // 목적: 기본 비동기 작업 (5초 소요)
    @Async
    public CompletableFuture<String> processData()  {
        try {
            log.info("작업시작 : {}", LocalDateTime.now());
            Thread.sleep(5000);
            log.info("Thread Name : {}",  Thread.currentThread().getName());

            log.info("작업종료 : {}", LocalDateTime.now());
            return CompletableFuture.completedFuture("SimpleAsyncService");

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    // 목적: 사용자 1명 처리 (개별 작업)
    @Async
    public CompletableFuture<String> processUser(String userName) {

        try {
            long startTime = System.currentTimeMillis();
            log.info("[{}] 작업시작 ", userName);
            int randomSeconds = (int)(Math.random() * 3) + 1;

            Thread.sleep(randomSeconds * 1000);

            log.info("Thread Name : {}",  Thread.currentThread().getName());

            long endTime =  System.currentTimeMillis();
            log.info("[{}] 작업 처리 시간 : {}", userName ,endTime-startTime);
            return CompletableFuture.completedFuture(userName + "-데이터 처리 완료!");

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

   // 목적: 여러 사용자 배치 처리
    @Async
    public CompletableFuture<String> processBatch(List<String> userNames) {
        log.info("배치 처리 시작 - 대상: {}", userNames);
        long startTime = System.currentTimeMillis();
        for(String name : userNames) {
            processUser(name);
        }
        long endTime =  System.currentTimeMillis();
        log.info("유저수:{} , 작업 처리 시간 : {}", userNames.size() ,endTime-startTime);
        return CompletableFuture.completedFuture(userNames + "-데이터 처리 완료!");
    }

   // 목적: 동기 처리 버전 (비교 실험용)
    public CompletableFuture<String> processSync(List<String> userNames) throws InterruptedException {

        log.info("Thread Start %s---------", LocalDateTime.now());
        Thread.sleep(5000);
        log.info("Thread Name : {}",  Thread.currentThread().getName());

        log.info("Thread End %s---------", LocalDateTime.now());
        return CompletableFuture.completedFuture("SimpleAsyncService");
    }




}
