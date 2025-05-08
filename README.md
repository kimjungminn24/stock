> 본 내용은 인프런 강의 [재고 시스템으로 알아보는 동시성 이슈 해결방안](https://www.inflearn.com/course/동시성이슈-재고시스템) 을 수강하며 정리한 학습 기록입니다.

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

2. Optimistic Lock (낙관적 락)

- 실제로 lock을 이용하지 않고 버전을 이용합으로써 정합성을 맞추는 방법
- 먼저 데이터를 읽은 후 update를 수행할 때 현재 내가 읽은 버전이 맞는지 확인하여 업데이트한다.
- 내가 읽은 버전이 수정사항이 생겼을 경우에는 application에서 다시 읽은 후에 작업을 수행해야한다.
- 별도의락을 잡지 않으므로 성능상 이점이 있다.
- 하지만 실패했을 때 재시도 로직을 작성해야한다. 따라서 충돌 가능성이 낮은 환경에서 유리하다.

```java
//jpa 사용방법
 @Lock(LockModeType.OPTIMISTIC)
```

3. Named Lock

- 이름을 가진 metadata locking이다.
- 실무에서는 데이터소스를 분리해서 사용한다. (커넥션풀이 부족해지는 경우가 있기때문에 다른 서비스에 영향이 갈 수 있다.) -> DB커넥션 풀을 두 개 이상으로 나눠서 운영한다.
- 이름을 가진 lock을 획득한 후 해제할 때 까지 다른 세션은 이 lock을 획득할 수 없다.
- DB 테이블 자체에 락을 거는 것이 아니라, MySQL의 내부 별도 공간에 이름(key) 기반으로 락을 거는 방식이다.
- 트랜잭션이 종료될 때 락이 자동으로 해제되지 않으므로 별도의 명령어로 해제를 수행하거나 락을 얻어온 세션이 종료되었을 때 자동으로 해제된다.
- 비관적 락은 타임아웃 제어가 어렵지만, 네임드 락은 GET_LOCK(key, timeout)으로 타임아웃을 명시적으로 설정할 수 있어 구현이 쉽다.

```java
@Query(value = "select get_lock(:key,3000)", nativeQuery = true)
void getLock(String key);

@Query(value = "select relaseKey(:key)", nativeQuery = true)
void releaseLock(String key);

```

- facde? : 복잡한 내부 로직을 간단한 인터페이스로 감싸는 디자인 패턴
- 분산락? : 분산 시스템 환경에서 , 동시에 같은 자원에 접근하지 못하도록 락을 거는것

- @Transactional(propagation = Propagation.REQUIRES_NEW)으로 변경한 이유는 기본 전파 방식(REQUIRED)일 경우 get_lock()과 stockService.decrease()가 하나의 트랜잭션으로 묶이기 때문이다. 이 경우 예외가 발생하거나 커밋 전에 메서드가 종료되면 락이 예상보다 빨리 풀릴 수 있다. REQUIRES_NEW를 사용하면 기존 트랜잭션과는 별도로 새로운 트랜잭션이 생성되며, 락 획득은 이 새로운 트랜잭션 내에서 실행되고 커밋된다. 이렇게 하면 락이 완전히 잡힌 후에 비즈니스 로직이 수행되므로 동시성 문제가 줄어든다.

## 4. Redis를 이용한 동시성 제어 방법

1. Lecttuce라이브러리

- setnx 명령어를 활용하여 분산락을 구현한다.
- setnx : SET if Not eXists : 해당 키가 존재하지 않을 때만 값을 설정
- 락이 이미 존재하면 계속 재시도하는 Spin Lock 방식으로 동작한다.
- 재시도하는 로직을 작성해야한다.
- 구현이 간단하다.
- spring data redis를 사용하면 lettuce가 기본이기 때문에 별도의 라이브러리를 사용하지 않아도된다.
- spin lock 방식이기 때문에 동시에 많은 스레드가 락 획득 대기 상태라면 redis에 부하가 갈 수 있다.

```shell
# Redis 컨테이너 실행
docker run --name myredis -d -p 6379:6379 redis

# CLI 접속
docker exec -it <container_id> redis-cli

# 락 설정 시도
setnx lock_key "locked"  # 결과: 1이면 성공, 0이면 실패

# 락 해제
del lock_key

```

2. Redisson 라이브러리

- pub-sub 기반으로 락 구현을 제공한다.
- 락을 점유한 스레드가 락을 해제하면, Redis를 통해 대기 중인 스레드에게 해제 신호를 전파하여 락을 사용할 수 있도록 한다.
- 이 방식은 Spin Lock과 달리 불필요한 재시도 없이 효율적으로 락을 점유할 수 있다.
- 라이브러리 사용시 직접 sub/pub 코드를 작성하지 않아도됨
- 락 획득 재시도를 기본으로 제공한다.
- pub/sub 방식으로 구현되어 있기 때문에 lettuce와 비교했을 때 부하가 덜 간다.
- lock 을 라이브러리 차원에서 제공해주기 때문에 사용법을 공부해야한다.

```shell
SUBSCRIBE ch1
PUBLISH ch1 "hello world"
```

-> 실무에서는 재시도가 필요하지 않은 lock은 lettuce
-> 재시도가 필요한 경우에는 redisson을 사용
