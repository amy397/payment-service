package mzc.shopping.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String paymentKey;
    private String transactionId;
    private String cancelReason;
    private BigDecimal refundAmount;

    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void complete(String paymentKey, String transactionId) {
        this.status = PaymentStatus.COMPLETED;
        this.paymentKey = paymentKey;
        this.transactionId = transactionId;
        this.paidAt = LocalDateTime.now();
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
    }

    public void cancel(String reason) {
        if (this.status != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("완료된 결제만 취소할 수 있습니다.");
        }
        this.status = PaymentStatus.CANCELLED;
        this.cancelReason = reason;
        this.cancelledAt = LocalDateTime.now();
    }

    public void refund(BigDecimal refundAmount, String reason) {
        if (this.status != PaymentStatus.COMPLETED && this.status != PaymentStatus.CANCELLED) {
            throw new IllegalStateException("완료되거나 취소된 결제만 환불할 수 있습니다.");
        }
        this.status = PaymentStatus.REFUNDED;
        this.refundAmount = refundAmount;
        this.cancelReason = reason;
        this.cancelledAt = LocalDateTime.now();
    }
}
