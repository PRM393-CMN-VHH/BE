package prm393.group8.flowermanagement.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
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
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
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
        String vnp_TmnCode = VNP_TMN_CODE;
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
        vnp_Params.put("vnp_ReturnUrl", VNP_RETURN_URL);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        String normalizedBankCode = normalizeBankCode(bankCode);
        if (normalizedBankCode != null) {
            vnp_Params.put("vnp_BankCode", normalizedBankCode);
        }

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        // Build hash data và query string trong cùng 1 vòng loop
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        try {
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    // Build hash data
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                    // Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Lỗi khi mã hóa URL parameters", e);
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = hmacSHA512(VNP_HASH_SECRET, hashData.toString());
        logger.info("VNPay create payment hashData={}", hashData);
        logger.info("VNPay create payment secureHash={}", vnp_SecureHash);
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return VNP_URL + "?" + queryUrl;
    }

    @Override
    public Map<String, String> handleVnPayCallback(Map<String, String> params) {
        Map<String, String> result = new HashMap<>();

        String vnp_SecureHash = params.get("vnp_SecureHash");
        params.remove("vnp_SecureHash");
        if (params.containsKey("vnp_SecureHashType")) {
            params.remove("vnp_SecureHashType");
        }

        // Build hash data theo đúng code mẫu VNPay
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        try {
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Lỗi khi mã hóa URL parameters cho callback", e);
        }

        String calculatedHash = hmacSHA512(VNP_HASH_SECRET, hashData.toString());
        logger.info("VNPay callback receivedHash={}, calculatedHash={}", vnp_SecureHash, calculatedHash);

        if (vnp_SecureHash != null && vnp_SecureHash.equals(calculatedHash)) {
            String responseCode = params.get("vnp_ResponseCode");

            if ("00".equals(responseCode)) {
                result.put("status", "success");
                result.put("message", "Thanh toán thành công");
                result.put("orderId", params.get("vnp_TxnRef"));
                result.put("bankCode", params.get("vnp_BankCode"));
                result.put("amount", params.get("vnp_Amount"));
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
