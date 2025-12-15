package mzc.shopping.payment.dto;

import lombok.*;
import mzc.shopping.payment.entity.Payment;
import mzc.shopping.payment.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private Long id;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String paymentMethod;
    private PaymentStatus status;
    private String paymentKey;
    private String transactionId;
    private String cancelReason;
    private BigDecimal refundAmount;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .paymentKey(payment.getPaymentKey())
                .transactionId(payment.getTransactionId())
                .cancelReason(payment.getCancelReason())
                .refundAmount(payment.getRefundAmount())
                .paidAt(payment.getPaidAt())
                .cancelledAt(payment.getCancelledAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
