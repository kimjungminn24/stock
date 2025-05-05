package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class StockService {

    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Transactional
    public void decrease(Long id, Long quantity) {
        //stock 조회
        Stock stock = stockRepository.findById(id).orElseThrow();

        //재고 감소
        stock.decrease(quantity);

        //갱신된 값 저장
        stockRepository.saveAndFlush(stock);
    }
}
