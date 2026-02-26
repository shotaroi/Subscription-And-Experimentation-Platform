package com.subscriptionplatform.subscription.repository;

import com.subscriptionplatform.subscription.domain.Subscription;
import com.subscriptionplatform.subscription.domain.SubscriptionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    @Query("SELECT s FROM Subscription s WHERE s.userId = :userId AND s.status NOT IN ('CANCELED', 'EXPIRED')")
    Optional<Subscription> findActiveByUserId(@Param("userId") UUID userId);

    @Query("SELECT s FROM Subscription s WHERE s.userId = :userId ORDER BY s.createdAt DESC")
    List<Subscription> findLatestByUserId(@Param("userId") UUID userId, Pageable pageable);

    default Optional<Subscription> findLatestByUserId(UUID userId) {
        return findLatestByUserId(userId, Pageable.ofSize(1)).stream().findFirst();
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subscription s WHERE s.id = :id")
    Optional<Subscription> findByIdForUpdate(@Param("id") UUID id);
}
