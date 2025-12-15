package mzc.shopping.payment.service;

import lombok.RequiredArgsConstructor;
import mzc.shopping.payment.client.OrderResponse;
import mzc.shopping.payment.client.OrderServiceClient;
import mzc.shopping.payment.dto.PaymentRequest;
import mzc.shopping.payment.dto.PaymentResponse;
import mzc.shopping.payment.dto.RefundRequest;
import mzc.shopping.payment.entity.Payment;
import mzc.shopping.payment.entity.PaymentStatus;
import mzc.shopping.payment.exception.PaymentFailedException;
import mzc.shopping.payment.exception.PaymentNotFoundException;
import mzc.shopping.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderServiceClient orderServiceClient;

    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {
        OrderResponse order = orderServiceClient.getOrder(request.getOrderId());

        if (paymentRepository.existsByOrderId(request.getOrderId())) {
            throw new PaymentFailedException("이미 해당 주문에 대한 결제가 존재합니다.");
        }

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        return PaymentResponse.from(savedPayment);
    }

    @Transactional
    public PaymentResponse confirmPayment(Long id) {
        Payment payment = findPaymentById(id);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentFailedException("대기 중인 결제만 승인할 수 있습니다.");
        }

        String paymentKey = "PK_" + UUID.randomUUID().toString();
        String transactionId = "TXN_" + System.currentTimeMillis();

        payment.complete(paymentKey, transactionId);
        orderServiceClient.updateOrderStatus(payment.getOrderId(), "CONFIRMED");

        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse cancelPayment(Long id, String reason) {
        Payment payment = findPaymentById(id);
        payment.cancel(reason);
        orderServiceClient.updateOrderStatus(payment.getOrderId(), "CANCELLED");
        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse refundPayment(Long id, RefundRequest request) {
        Payment payment = findPaymentById(id);
        payment.refund(request.getRefundAmount(), request.getReason());
        return PaymentResponse.from(payment);
    }

    public PaymentResponse getPayment(Long id) {
        return PaymentResponse.from(findPaymentById(id));
    }

    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("주문 ID " + orderId + "에 대한 결제를 찾을 수 없습니다."));
        return PaymentResponse.from(payment);
    }

    public List<PaymentResponse> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }

    public List<PaymentResponse> getPaymentsByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status).stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }

    private Payment findPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("결제 ID " + id + "를 찾을 수 없습니다."));
    }
}
