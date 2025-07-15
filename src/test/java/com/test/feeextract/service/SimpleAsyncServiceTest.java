package com.test.feeextract.service;

import com.test.feeextract.domain.JobStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class SimpleAsyncServiceTest {

    SimpleAsyncService jobManager = new SimpleAsyncService();

    @Test
    @DisplayName("잡 생성")
    void 잡생성하기(){
        String jobId = jobManager.createJob("잡생성~");
        assertEquals(jobId, jobManager.getJob(jobId).getJobId());
        assertEquals(jobManager.getJob(jobId).getStatus(), JobStatus.RUNNING);
    }

    @Test
    @DisplayName("잡 성공처리")
    void 잡_성공처리(){
        String jobId = jobManager.createJob("잡생성");
        jobManager.completeJob(jobId,"성공처리");
        assertEquals(jobManager.getJob(jobId).getStatus(), JobStatus.COMPLETED);
    }

    @Test
    @DisplayName("잡 실패처리")
    void 잡_실패처리(){
        String jobId = jobManager.createJob("잡생성");
        jobManager.failJob(jobId,"실패임");
        assertEquals(jobManager.getJob(jobId).getStatus(), JobStatus.FAILED);
    }

    @Test
    @DisplayName("잡 업데이트처리")
    void 잡_업데이트처리(){
        String jobId = jobManager.createJob("잡생성");
        jobManager.updateJob(jobId,JobStatus.RUNNING,30,"조회중");
        jobManager.updateJob(jobId,JobStatus.RUNNING,50,"조회중");
        jobManager.updateJob(jobId,JobStatus.RUNNING,70,"조회중");

        assertEquals(jobManager.getJob(jobId).getStatus(), JobStatus.RUNNING);
        assertEquals(jobManager.getJob(jobId).getProgress(), 70);
    }


}