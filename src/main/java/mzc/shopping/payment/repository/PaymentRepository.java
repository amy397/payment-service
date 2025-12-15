package mzc.shopping.payment.repository;

import mzc.shopping.payment.entity.Payment;
import mzc.shopping.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    List<Payment> findByUserId(Long userId);

    List<Payment> findByUserIdAndStatus(Long userId, PaymentStatus status);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByOrderId(Long orderId);
}
