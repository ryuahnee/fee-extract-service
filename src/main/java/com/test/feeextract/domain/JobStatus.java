package com.test.feeextract.domain;

// Job의 상태값
public enum JobStatus {
    WAITING("대기중"),
    RUNNING("진행중"),
    COMPLETED("완료"),
    FAILED("실패");

    private final String description;

    JobStatus(String description) {
        this.description = description;
    }

    // 현재 상태 체크
    public String getDescription() {
        return this.description;
    }
    //완료 되었는지?
    public boolean isCompleted() {
        return this == COMPLETED;
    }
    // 종료 상태인지? (완료 또는 실패)
    public boolean isFailed() {
        return this == FAILED || this == COMPLETED;
    }

    // 진행 상태인지?
    public boolean isActive() {
        return this == WAITING || this == RUNNING;
    }

}
