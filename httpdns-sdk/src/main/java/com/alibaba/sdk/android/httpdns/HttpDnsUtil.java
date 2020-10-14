package com.alibaba.sdk.android.httpdns;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

class HttpDnsUtil {
    private static final String rex = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
            + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\." + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
            + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";
    private static Pattern pattern = Pattern.compile(rex);

    static boolean isAHost(String host) {
        try {
            if (host != null) {
                char[] bytes = host.toCharArray();
                if (bytes.length <= 0 || bytes.length > 255) {
                    return false;
                }
                for (char aByte : bytes) {
                    if (!((aByte >= 'A' && aByte <= 'Z') || (aByte >= 'a' && aByte <= 'z')
                            || (aByte >= '0' && aByte <= '9') || aByte == '.' || aByte == '-')) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    static boolean isAnIP(String ip) {
        return !(ip == null || ip.length() < 7 || ip.length() > 15 || ip.equals("")) && pattern.matcher(ip).matches();
    }

    static String getMD5String(final String s) throws NoSuchAlgorithmException {
        final String MD5 = "MD5";
        // Create MD5 Hash
        MessageDigest digest = MessageDigest.getInstance(MD5);
        digest.update(s.getBytes());
        byte messageDigest[] = digest.digest();

        // Create Hex String
        StringBuilder hexString = new StringBuilder();
        for (byte aMessageDigest : messageDigest) {
            String h = Integer.toHexString(0xFF & aMessageDigest);
            while (h.length() < 2)
                h = "0" + h;
            hexString.append(h);
        }
        return hexString.toString();
    }

    static String getSHA1String(String s) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        byte[] res = sha1.digest(s.getBytes());
        BigInteger big = new BigInteger(1, res);
        return big.toString(16);
    }

}
