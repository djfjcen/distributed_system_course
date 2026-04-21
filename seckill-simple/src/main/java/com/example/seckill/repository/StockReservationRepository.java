package com.example.seckill.repository;

import com.example.seckill.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
}
