package com.example.seckill.repository;

import com.example.seckill.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByUserIdAndProductId(Long userId, Long productId);

    List<Order> findByUserId(Long userId);

    List<Order> findByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :orderId")
    Optional<Order> findByIdForUpdate(Long orderId);

    @Modifying
    @Query("update Order o set o.status = :toStatus where o.id = :orderId and o.userId = :userId and o.status = :fromStatus")
    int moveStatus(@Param("orderId") Long orderId,
                   @Param("userId") Long userId,
                   @Param("fromStatus") Integer fromStatus,
                   @Param("toStatus") Integer toStatus);
}
