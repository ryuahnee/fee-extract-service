package com.test.feeextract.service;

import com.test.feeextract.domain.JobInfo;
import com.test.feeextract.domain.JobStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
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
