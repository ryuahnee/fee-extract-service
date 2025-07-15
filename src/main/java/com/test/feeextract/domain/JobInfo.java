package com.test.feeextract.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class JobInfo {
    private String jobId;        // 작업 고유 ID
    private JobStatus status;    // 현재 상태
    private String taskName;
    private int progress;        // 진행률 (0-100)
    private String message;      // 상태 메시지
    private LocalDateTime startTime;  // 시작 시간
    private LocalDateTime endTime;    // 종료 시간

}
