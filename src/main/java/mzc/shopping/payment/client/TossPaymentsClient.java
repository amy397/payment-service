package mzc.shopping.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "toss-payments", url = "https://api.tosspayments.com")
public interface TossPaymentsClient {

    @PostMapping("/v1/payments/confirm")
    Map<String, Object> confirmPayment(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Object> request
    );
}
