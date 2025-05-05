> W: 본 내용은 인프런 강의  
> [재고 시스템으로 알아보는 동시성 이슈 해결방안](https://www.inflearn.com/course/동시성이슈-재고시스템) 을 수강하며 정리한 학습 기록입니다.

## 2. 재고 시스템 만들어보기

- 동시에 여러 스레드가 하나의 재고 데이터를 읽고 수정하려고 하기 때문에 문제가 발생한다.
- Race Condition(경쟁 상태)는 멀티 스레드 환경에서 동일한 자원에 대한 접근이 제어하지 않아 생기는 문제

```java
// 멀티 스레드를 실행하기 위한 자바 표준 API
// 동시에 최대 32개 스레드가 실행됨
ExecutorService executor = Executors.newFixedThreadPool(32);

//여러 스레드가 작업을 마칠 때까지 메인스레드가 기다리게 만드는 클래스
CountDownLatch latch = new CountDownLatch(100); // 초기값
latch.

countDown();  // 작업 완료
latch.

await();      // 대기
```
