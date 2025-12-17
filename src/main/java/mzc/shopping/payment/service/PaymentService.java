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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import mzc.shopping.payment.client.TossPaymentsClient;
import mzc.shopping.payment.dto.TossPaymentRequest;
import org.springframework.beans.factory.annotation.Value;
import java.util.Base64;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderServiceClient orderServiceClient;
    private final TossPaymentsClient tossPaymentsClient;
    @Value("${toss.secret-key}")
    private String tossSecretKey;


    @Transactional
    public PaymentResponse confirmTossPayment(PaymentRequest request) {
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

    @Transactional
    public PaymentResponse confirmTossPayemnt(TossPaymentRequest request) {

        // 1. Authorization 헤더 생성
        String encodedKey = Base64.getEncoder().encodeToString((tossSecretKey + ":").getBytes());
        String authorization = "Basic " + encodedKey;
        // 2. 요청 바디 생성
        Map<String, Object> body = new HashMap<>();
        body.put("paymentKey", request.getPaymentKey());
        body.put("orderId", request.getOrderId());
        body.put("amount", request.getAmount());


        try {
            // 3. Feign Client로 토스 API 호출
            Map<String, Object> tossResponse = tossPaymentsClient.confirmPayment(authorization, body);

            // 4. orderId에서 실제 주문 ID 추출 (ORDER_123_timestamp 형식)
            String[] parts = request.getOrderId().split("_");
            Long actualOrderId = Long.parseLong(parts[1]);

            // 5. 결제 정보 DB 저장
            Payment payment = Payment.builder()
                    .orderId(actualOrderId)
                    .userId(getOrderUserId(actualOrderId))
                    .amount(new java.math.BigDecimal(request.getAmount()))
                    .paymentMethod((String) tossResponse.get("method"))
                    .status(PaymentStatus.COMPLETED)
                    .paymentKey(request.getPaymentKey())
                    .transactionId((String) tossResponse.get("transactionKey"))
                    .build();

            Payment saved = paymentRepository.save(payment);

            // 6. 주문 상태 업데이트
            orderServiceClient.updateOrderStatus(actualOrderId, "CONFIRMED");

            return PaymentResponse.from(saved);

        } catch (Exception e) {
            throw new PaymentFailedException("토스페이먼츠 결제 승인 실패: " + e.getMessage());
        }
    }

    // 주문에서 userId 가져오기
    private Long getOrderUserId(Long orderId) {
        try {
            OrderResponse order = orderServiceClient.getOrder(orderId);
            return order.getUserId();
        } catch (Exception e) {
            return 1L;
        }
    }

}
