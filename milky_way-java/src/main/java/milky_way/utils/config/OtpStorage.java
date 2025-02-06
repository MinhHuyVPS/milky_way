package milky_way.utils.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class OtpStorage {
    private static final Map<String, OtpEntry> otpCache = new ConcurrentHashMap<>();
    private static final Map<String, String> otpToEmailMap = new ConcurrentHashMap<>();


    public static void storeOtp(String email, String otp) {
        long expirationTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(15);
        otpCache.put(email, new OtpEntry(otp, expirationTime));
        otpToEmailMap.put(otp, email);
    }

    public static String getOtp(String email) {
        OtpEntry entry = otpCache.get(email);
        return (entry != null && entry.getExpirationTime() > System.currentTimeMillis())
                ? entry.getOtp() : null;
    }

    public static String getEmailByOtp(String otp) {
        return otpToEmailMap.get(otp);
    }

    public static void removeOtp(String email) {
        OtpEntry entry = otpCache.remove(email);
        if (entry != null) {
            otpToEmailMap.remove(entry.getOtp());
        }
    }

    @Getter
    @NoArgsConstructor(force = true)
    @AllArgsConstructor
    public static class OtpEntry {
        private final String otp;
        private final long expirationTime;

    }
}
