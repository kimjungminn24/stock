package com.example.stock.service;


import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class StockServiceTest {

    @Autowired
    private PessimisticLockStockService stockService;

    @Autowired
    private StockRepository stockRepository;


    @BeforeEach
    public void before() {
        stockRepository.saveAndFlush(new Stock(1L, 100L));
    }

    @AfterEach
    public void after() {
        stockRepository.deleteAll();
    }


    @Test
    public void 재고감소() {
        stockService.decrease(1L, 1L);

        //100 -1 =99
        Stock stock = stockRepository.findById(1L).orElseThrow();

        assertEquals(99, stock.getQuantity());
    }

    @Test
    public void 동시에_100개_요청() throws InterruptedException {
        int threadCount = 100;

        // 동시에 여러 요청을 보낼 쓰레드 풀 생성 (최대 32개의 쓰레드 동시 실행)
        ExecutorService executorService = Executors.newFixedThreadPool(32);

        // 모든 쓰레드의 작업 완료를 기다리기 위한 CountDownLatch 생성
        // 쓰레드 수만큼 초기값 설정
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 100개의 요청을 비동기로 실행
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> { //각 쓰레드에서 실행할 작업 정의
                try {
                    stockService.decrease(1L, 1L);
                } finally {
                    latch.countDown(); //현재 쓰레드 작업 완료
                }
            });
        }

        //모든 쓰레드가 작업을 마칠때 까지 대기 (latch가 0이 될 때까지)
        latch.await();

        Stock stock = stockRepository.findById(1L).orElseThrow();

        //기대결과 : 100-(1*100)=0
        assertEquals(0, stock.getQuantity());

        //두 개 이상의 스레드가 공유자원에 접근하러 변경할 때 레이스 컨디션이 발생할 수 있음
    }


}