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

## 3. DB를 이용한 동시성 제어 방법

1. Pessimistic Lock (비관적 락)

- 실제로 데이터에 Lock을 걸어서 정합성을 맞추는 방법
- exclusive lock(배타적락)을 걸게 되면 다른 트랜잭션에서는 lock이 해제되기 전에 데이터를 가져갈 수 없게 된다.
- 데드락이 걸릴 수 있기 때문에 조심해야한다.
- 데드락 : 둘 이상의 프로세스가 서로 다른 자원을 점유한채, 상대방의 자원을 기다리는 상황
- 충돌이 많지 않다면, 락을 걸지 않는 낙관적 락보다 오히려 성능이 떨어질 수 있음 (충돌 가능성이 높은 환경이 좋다)

```java
//jpa 사용방법
@Lock(LockModeType.PESSIMISTIC_WRITE)
```

```sql
select s1_0.id,s1_0.product_id,s1_0.quantity
from stock s1_0
where s1_0.id=?
for update # 해당 행에 락을 걸고 완료전까지 다른 트랜잭션이 읽을수는 있어도 수정하지 못하게 막는다.

```

