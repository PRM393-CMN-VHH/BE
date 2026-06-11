package prm393.group8.flowermanagement.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class OtpService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final long OTP_VALIDITY_DURATION = 5; // 5 minutes

    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 6-digit OTP
        return String.valueOf(otp);
    }

    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Mã xác thực OTP đăng ký tài khoản - Tiệm Hoa Xinh");
        message.setText("Chào bạn,\n\nMã OTP để xác thực đăng ký tài khoản của bạn là: " + otp + "\n\nMã này có hiệu lực trong 5 phút.\n\nTrân trọng,\nĐội ngũ Tiệm Hoa Xinh.");
        mailSender.send(message);
    }

    public void saveOtpToRedis(String email, String otp) {
        redisTemplate.opsForValue().set("OTP_" + email, otp, OTP_VALIDITY_DURATION, TimeUnit.MINUTES);
    }

    public boolean verifyOtp(String email, String otp) {
        String savedOtp = redisTemplate.opsForValue().get("OTP_" + email);
        if (savedOtp != null && savedOtp.equals(otp)) {
            // Delete OTP after successful verification to prevent reuse
            redisTemplate.delete("OTP_" + email);
            return true;
        }
        return false;
    }
}
