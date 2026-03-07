package com.test.feeextract.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JobStatus Enum 테스트")
class JobStatusTest {

    @Test
    @DisplayName("isFailed() - FAILED 상태만 true 반환")
    void isFailed_FAILED상태만_true() {
        assertTrue(JobStatus.FAILED.isFailed());
        assertFalse(JobStatus.COMPLETED.isFailed());
        assertFalse(JobStatus.RUNNING.isFailed());
        assertFalse(JobStatus.WAITING.isFailed());
    }

    @Test
    @DisplayName("isCompleted() - COMPLETED 상태만 true 반환")
    void isCompleted_COMPLETED상태만_true() {
        assertTrue(JobStatus.COMPLETED.isCompleted());
        assertFalse(JobStatus.FAILED.isCompleted());
        assertFalse(JobStatus.RUNNING.isCompleted());
        assertFalse(JobStatus.WAITING.isCompleted());
    }

    @Test
    @DisplayName("isTerminated() - COMPLETED, FAILED 상태에서 true 반환")
    void isTerminated_완료및실패시_true() {
        assertTrue(JobStatus.COMPLETED.isTerminated());
        assertTrue(JobStatus.FAILED.isTerminated());
        assertFalse(JobStatus.RUNNING.isTerminated());
        assertFalse(JobStatus.WAITING.isTerminated());
    }

    @Test
    @DisplayName("isActive() - WAITING, RUNNING 상태에서 true 반환")
    void isActive_대기및진행중_true() {
        assertTrue(JobStatus.WAITING.isActive());
        assertTrue(JobStatus.RUNNING.isActive());
        assertFalse(JobStatus.COMPLETED.isActive());
        assertFalse(JobStatus.FAILED.isActive());
    }

    @Test
    @DisplayName("getDescription() - 각 상태별 한글 설명 반환")
    void getDescription_한글설명_반환() {
        assertEquals("대기중", JobStatus.WAITING.getDescription());
        assertEquals("진행중", JobStatus.RUNNING.getDescription());
        assertEquals("완료", JobStatus.COMPLETED.getDescription());
        assertEquals("실패", JobStatus.FAILED.getDescription());
    }
}
