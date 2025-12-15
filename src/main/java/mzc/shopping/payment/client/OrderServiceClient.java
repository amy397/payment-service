package mzc.shopping.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service", url = "${service.order.url}")
public interface OrderServiceClient {

    @GetMapping("/api/orders/{id}")
    OrderResponse getOrder(@PathVariable("id") Long id);

    @PatchMapping("/api/orders/{id}/status")
    OrderResponse updateOrderStatus(@PathVariable("id") Long id, @RequestParam("status") String status);
}
