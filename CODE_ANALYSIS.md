# fee-extract-service 코드 분석 보고서

> 분석일: 2026-03-07
> 브랜치: `claude/code-analysis-9ZtBA`

---

## 1. 프로젝트 개요

| 항목 | 내용 |
|------|------|
| 프레임워크 | Spring Boot 3.4.7 |
| 언어 | Java 17 |
| 빌드 도구 | Gradle 8.x |
| DB | H2 (인메모리, 테스트용) |
| ORM | Spring Data JPA + MyBatis |
| 테스트 | JUnit 5 |

이 프로젝트는 **비동기 처리 패턴과 대용량 데이터 메모리 효율을 학습·시연하기 위한 Spring Boot 프로토타입**입니다.
실제 수수료 추출(Fee Extract) 비즈니스 로직은 아직 구현되지 않은 상태이며, 현재는 비동기 처리 인프라와 메모리 모니터링 기반이 구축된 단계입니다.

---

## 2. 디렉터리 구조

```
src/main/java/com/test/feeextract/
├── FeeextractApplication.java          # Spring Boot 진입점
├── config/
│   └── AsyncConfig.java                # 비동기 ThreadPool 설정
├── controller/
│   ├── AsyncController.java            # 비동기 작업 REST API
│   └── MemoryTestController.java       # 메모리 테스트 REST API
├── domain/
│   ├── JobInfo.java                    # 작업 메타데이터 DTO
│   └── JobStatus.java                  # 작업 상태 Enum
├── entity/
│   └── User.java                       # JPA 엔티티
├── service/
│   ├── SimpleAsyncService.java         # 핵심 비동기 작업 관리
│   └── LargeDataService.java           # 대용량 데이터 처리
└── util/
    └── MemoryMonitor.java              # 메모리 모니터링 유틸
```

---

## 3. 핵심 컴포넌트 상세 분석

### 3-1. `FeeextractApplication.java` — 진입점

```java
public static void main(String[] args) {
    JobStatus com = JobStatus.COMPLETED;
    System.out.println("1.com: " + com.getDescription());
    // ... (enum 테스트 코드)
    int randomSeconds = (int)(Math.random() * 3) + 1;
    SpringApplication.run(FeeextractApplication.class, args);
}
```

**문제점:**
- `main()` 메서드 내부에 `JobStatus` Enum 동작 검증 코드와 난수 생성 코드가 혼재되어 있음
- 이 코드들은 서비스 실행과 무관한 실험용 코드이며, 운영 코드에 포함되어서는 안 됨
- `(int)Math.random() * 3 + 1` 계산식에 **버그** 존재: 연산자 우선순위로 인해 항상 1이 반환됨. 올바른 식은 `(int)(Math.random() * 3) + 1`이며 아래 줄에서는 올바르게 사용됨

---

### 3-2. `AsyncConfig.java` — 비동기 설정

```java
executor.setCorePoolSize(1);
executor.setMaxPoolSize(2);
```

**문제점:**
- ThreadPool 크기가 매우 작음 (core=1, max=2)
- 동시 요청이 3개 이상이면 큐에 쌓이거나 요청이 거부될 수 있음
- `setQueueCapacity()`, `setThreadNamePrefix()`, `setRejectedExecutionHandler()` 등 필수 설정 누락
- 운영 환경에서는 서버 사양에 맞춰 풀 크기 조정 필요

---

### 3-3. `SimpleAsyncService.java` — 작업 관리 서비스

**역할:** Job 생성, 상태 업데이트, 조회, 완료/실패 처리, 자동 정리

**긍정적인 점:**
- `ConcurrentHashMap`으로 thread-safe한 인메모리 Job 저장소 구현
- `computeIfPresent()`로 원자적(atomic) 상태 업데이트
- `@Scheduled`로 완료/실패된 오래된 Job(10분 이상)을 5분마다 자동 정리 → 메모리 누수 방지
- 1분마다 JVM 메모리 상태를 로그로 출력하는 모니터링 스케줄러 포함

**문제점:**
- **영속성 부재:** 서버 재시작 시 모든 Job 정보가 소실됨
- `processBatch()` 내부에서 `processUser()`를 직접 호출하여 비동기 Self-Invocation 문제 발생
  → Spring AOP 프록시를 거치지 않아 `@Async`가 동작하지 않을 수 있음
- `processSync()` 메서드는 인자로 `List<String>`을 받지만 실제로는 사용하지 않음 (데드 코드)
- `createJob()` 시 `taskName` 파라미터를 받지만 `JobInfo` 빌더에 설정하지 않음

---

### 3-4. `LargeDataService.java` — 대용량 데이터 처리

두 가지 처리 방식을 비교하는 서비스:

| 방식 | 특징 | 메모리 |
|------|------|--------|
| `processLargeData()` | 전체 데이터를 메모리에 로딩 후 처리 | 높음 |
| `processLargeDataStreaming()` | 1,000건 단위 배치 처리 | 낮음 |

**긍정적인 점:**
- 두 방식의 메모리 사용량을 단계별로 측정하여 비교 가능
- `dataList.clear(); dataList = null;` 명시적 메모리 해제 패턴 사용
- 스트리밍 방식에서 50,000건마다 `System.gc()` 호출로 메모리 압박 완화 시도

**문제점:**
- `System.gc()` 호출은 JVM에 대한 힌트일 뿐이며 실제 GC 실행을 보장하지 않음
- 내부 `UserData` 정적 클래스가 실제 수수료(Fee) 도메인과 무관한 테스트 데이터
- `processDataList()`, `processBatch()` 두 메서드가 유사한 로직을 중복으로 구현

---

### 3-5. `JobStatus.java` — 상태 Enum

**문제점:**
- `isFailed()` 메서드의 로직 오류:

```java
// 현재 코드 (버그)
public boolean isFailed() {
    return this == FAILED || this == COMPLETED;  // COMPLETED를 실패로 판단하는 버그!
}
```

- `COMPLETED` 상태를 실패로 판단하는 명백한 논리 오류
- 올바른 구현: `return this == FAILED;`

---

### 3-6. `MemoryMonitor.java` — 메모리 모니터링

**긍정적인 점:**
- JVM `Runtime`을 활용한 Heap 사용량 측정
- 단계별 메모리 스냅샷 저장 및 이전 값과의 차이 계산

**문제점:**
- `measureMemory()` 호출 시마다 `System.gc()` 강제 실행 → 성능에 영향을 줄 수 있음
- `memorySnapshots` 필드가 `HashMap`으로 구현되어 있어 멀티스레드 환경에서 thread-unsafe
  → `ConcurrentHashMap`으로 교체 필요

---

## 4. REST API 목록

### AsyncController (`/async`)

| HTTP | 경로 | 설명 |
|------|------|------|
| GET | `/async/single` | 단일 비동기 작업 실행 (5초 소요) |
| GET | `/async/user/{name}` | 사용자 1명 비동기 처리 |
| POST | `/async/batch` | 사용자 목록 배치 처리 |

### MemoryTestController (`/memory-test`)

| HTTP | 경로 | 설명 |
|------|------|------|
| POST | `/memory-test/large-data` | 전통 방식 대용량 처리 |
| POST | `/memory-test/large-data-streaming` | 스트리밍 방식 대용량 처리 |
| GET | `/memory-test/job/{jobId}` | Job 상태 조회 |
| GET | `/memory-test/memory-status` | 현재 JVM 메모리 상태 |
| POST | `/memory-test/gc` | GC 강제 실행 |
| POST | `/memory-test/memory-comparison` | 다양한 크기별 메모리 비교 테스트 |

---

## 5. 테스트 현황

| 파일 | 테스트 내용 |
|------|------------|
| `FeeextractApplicationTests.java` | Spring 컨텍스트 로드 테스트 |
| `SimpleAsyncServiceTest.java` | Job 생성/완료/실패/진행률 업데이트 단위 테스트 |

**부족한 테스트:**
- Controller 레이어 통합 테스트 없음 (MockMvc 등)
- `LargeDataService` 단위 테스트 없음
- `MemoryMonitor` 단위 테스트 없음
- `JobStatus.isFailed()` 버그를 잡을 수 있는 테스트 없음

---

## 6. CI/CD 파이프라인

```
push to develop/main 또는 main PR
  └─ [test] JDK 17 → ./gradlew test
       └─ [build, main 브랜치 한정] ./gradlew build → JAR artifact 업로드
```

---

## 7. 발견된 버그 및 개선 제안 요약

| 우선순위 | 분류 | 이슈 | 개선 방안 |
|---------|------|------|----------|
| **긴급** | 버그 | `JobStatus.isFailed()` - COMPLETED를 실패로 판단 | `return this == FAILED;`로 수정 |
| **긴급** | 버그 | `FeeextractApplication.main()`의 `(int)Math.random() * 3 + 1` 연산 오류 | 실험 코드 제거 또는 괄호 교정 |
| **높음** | 설계 | `processBatch()` 내 Self-Invocation으로 `@Async` 미작동 | `ApplicationContext`로 프록시 통해 호출하거나 별도 서비스로 분리 |
| **높음** | 설계 | Job 상태 인메모리 저장 → 서버 재시작 시 소실 | Redis 또는 DB 기반 영속화 고려 |
| **중간** | 성능 | ThreadPool 크기 과소 설정 (core=1, max=2) | 운영 환경에 맞게 조정 |
| **중간** | 안전성 | `MemoryMonitor.memorySnapshots` - HashMap 사용 (thread-unsafe) | `ConcurrentHashMap`으로 교체 |
| **중간** | 품질 | `main()` 메서드 내 실험용 코드 혼재 | 별도 테스트 클래스로 분리 또는 제거 |
| **낮음** | 품질 | `createJob()`에서 `taskName` 설정 누락 | `JobInfo` 빌더에 `.taskName(taskName)` 추가 |
| **낮음** | 품질 | Controller/Service 테스트 부재 | 통합 테스트 및 단위 테스트 추가 |
| **낮음** | 구조 | `User` 엔티티와 실제 Fee 도메인 연관 설계 필요 | 비즈니스 요구사항에 맞는 도메인 모델 설계 |

---

## 8. 결론

이 서비스는 비동기 처리와 메모리 효율화를 학습하기 위한 **프로토타입 단계**에 있습니다.
핵심 기반 구조(비동기 처리, 메모리 모니터링, Job 관리)는 잘 구성되어 있으나,
다음 사항을 우선적으로 해결해야 합니다:

1. **`JobStatus.isFailed()` 버그 수정** — 즉시 수정 필요
2. **`@Async` Self-Invocation 문제 해결** — 배치 처리 기능이 의도대로 동작하지 않음
3. **실제 수수료 추출 비즈니스 로직 구현** — 현재는 테스트 데이터 처리 시뮬레이션만 존재
4. **테스트 커버리지 확대** — 특히 버그가 발견된 영역 중심으로 테스트 추가
