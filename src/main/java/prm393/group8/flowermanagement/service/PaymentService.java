package prm393.group8.flowermanagement.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public interface PaymentService {

    String createVnPayPayment(int orderId, long amount, String orderInfo, String bankCode, HttpServletRequest request);
    Map<String, String> handleVnPayCallback(Map<String, String> params);
}