package prm393.group8.flowermanagement.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

    @Value("${vnpay.tmn-code}")
    private String VNP_TMN_CODE;

    @Value("${vnpay.hash-secret}")
    private String VNP_HASH_SECRET;

    @Value("${vnpay.url}")
    private String VNP_URL;

    @Value("${vnpay.return-url}")
    private String VNP_RETURN_URL;

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmacSha512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.trim().getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmacSha512.init(secretKey);
            byte[] hash = hmacSha512.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo chữ ký HmacSHA512", e);
        }
    }

    private String encodeVnPay(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String buildVnPayHashData(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();

        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (!hashData.isEmpty()) {
                    hashData.append('&');
                }
                hashData.append(fieldName).append('=').append(encodeVnPay(fieldValue));
            }
        }

        return hashData.toString();
    }

    private String buildVnPayQuery(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder query = new StringBuilder();

        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (!query.isEmpty()) {
                    query.append('&');
                }
                query.append(encodeVnPay(fieldName)).append('=').append(encodeVnPay(fieldValue));
            }
        }

        return query.toString();
    }

    private String normalizeBankCode(String bankCode) {
        if (bankCode == null || bankCode.isBlank()) {
            return null;
        }

        String normalizedBankCode = bankCode.trim().toUpperCase(Locale.ROOT);
        if ("VNPAYQR".equals(normalizedBankCode)) {
            logger.warn("VNPay bankCode=VNPAYQR is not supported by this sandbox merchant; using VNPay default payment page.");
            return null;
        }

        return normalizedBankCode;
    }

    @Override
    public String createVnPayPayment(int orderId, long amount, String orderInfo, String bankCode, HttpServletRequest request) {

        String vnp_IpAddr = request.getRemoteAddr();
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = orderId + "_" + System.currentTimeMillis();
        String vnp_TmnCode = VNP_TMN_CODE.trim();
        String vnp_Amount = String.valueOf(amount * 100);

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", vnp_Amount);
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", orderInfo);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VNP_RETURN_URL.trim());
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
        
        String normalizedBankCode = normalizeBankCode(bankCode);
        if (normalizedBankCode != null) {
            vnp_Params.put("vnp_BankCode", normalizedBankCode);
        }

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        String queryUrl = buildVnPayQuery(vnp_Params);
        String hashData = buildVnPayHashData(vnp_Params);
        String vnp_SecureHash = hmacSHA512(VNP_HASH_SECRET, hashData);
        logger.info("VNPay create payment hashData={}", hashData);
        logger.info("VNPay create payment secureHash={}", vnp_SecureHash);
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return VNP_URL.trim() + "?" + queryUrl;
    }

    @Override
    public Map<String, String> handleVnPayCallback(Map<String, String> params) {
        Map<String, String> result = new HashMap<>();

        Map<String, String> vnp_Params = new HashMap<>(params);
        String vnp_SecureHash = vnp_Params.remove("vnp_SecureHash");
        vnp_Params.remove("vnp_SecureHashType");

        String hashData = buildVnPayHashData(vnp_Params);
        String calculatedHash = hmacSHA512(VNP_HASH_SECRET, hashData);
        logger.info("VNPay callback hashData={}", hashData);
        logger.info("VNPay callback receivedHash={}, calculatedHash={}", vnp_SecureHash, calculatedHash);

        if (vnp_SecureHash != null && vnp_SecureHash.equalsIgnoreCase(calculatedHash)) {
            String responseCode = vnp_Params.get("vnp_ResponseCode");

            if ("00".equals(responseCode)) {
                result.put("status", "success");
                result.put("message", "Thanh toán thành công");
                result.put("orderId", vnp_Params.get("vnp_TxnRef"));
                result.put("bankCode", vnp_Params.get("vnp_BankCode"));
                result.put("amount", vnp_Params.get("vnp_Amount"));
            } else {
                result.put("status", "fail");
                result.put("message", "Thanh toán thất bại (Mã lỗi: " + responseCode + ")");
            }
        } else {
            result.put("status", "invalid_signature");
            result.put("message", "Chữ ký không hợp lệ");
        }

        return result;
    }
}
