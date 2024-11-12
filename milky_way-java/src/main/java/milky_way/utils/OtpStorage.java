package milky_way.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class OtpStorage {
    private static final ConcurrentHashMap<String, OtpEntry> otpCache = new ConcurrentHashMap<>();

    public static void storeOtp(String email, String otp) {
        long expirationTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(15);
        otpCache.put(email, new OtpEntry(otp, expirationTime));
    }

    public static String getOtp(String email) {
        OtpEntry entry = otpCache.get(email);
        if (entry != null && entry.getExpirationTime() > System.currentTimeMillis()) {
            return entry.getOtp();
        }
        return null;
    }
    public static class OtpEntry {
        private final String otp;
        private final long expirationTime;

        public OtpEntry(String otp, long expirationTime) {
            this.otp = otp;
            this.expirationTime = expirationTime;
        }

        public String getOtp() {
            return otp;
        }

        public long getExpirationTime() {
            return expirationTime;
        }
    }
}
