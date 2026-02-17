package javafx_demo.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * ECDH 密钥交换 + AES-256-GCM 加解密 + HMAC-SHA256 签名
 * 使用 JDK 内置 SunEC 提供器，与后端 BouncyCastle 兼容（secp256r1 / X509 编码）
 */
public class CryptoUtil {

    private static final String CURVE = "secp256r1";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;

    /** 生成 ECDH 密钥对 */
    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec(CURVE));
        return kpg.generateKeyPair();
    }

    /** 公钥 → Base64 (X.509 编码) */
    public static String encodePublicKey(PublicKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /** Base64 → 公钥 */
    public static PublicKey decodePublicKey(String base64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("EC");
        return kf.generatePublic(spec);
    }

    /** ECDH 密钥协商 → AES-256 密钥 (SHA-256 派生) */
    public static SecretKey deriveSharedSecret(PrivateKey myPrivate, PublicKey otherPublic) throws Exception {
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(myPrivate);
        ka.doPhase(otherPublic, true);
        byte[] sharedSecret = ka.generateSecret();
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] derived = sha256.digest(sharedSecret);
        return new SecretKeySpec(derived, "AES");
    }

    /** AES-256-GCM 加密 → Base64(IV + ciphertext + tag) */
    public static String encrypt(SecretKey key, String plaintext) throws Exception {
        byte[] iv = new byte[GCM_IV_BYTES];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance(AES_GCM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] result = new byte[iv.length + ct.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(ct, 0, result, iv.length, ct.length);
        return Base64.getEncoder().encodeToString(result);
    }

    /** AES-256-GCM 解密 */
    public static String decrypt(SecretKey key, String encryptedBase64) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(encryptedBase64);
        byte[] iv = new byte[GCM_IV_BYTES];
        System.arraycopy(decoded, 0, iv, 0, GCM_IV_BYTES);
        byte[] ct = new byte[decoded.length - GCM_IV_BYTES];
        System.arraycopy(decoded, GCM_IV_BYTES, ct, 0, ct.length);
        Cipher cipher = Cipher.getInstance(AES_GCM);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
        return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
    }

    /** HMAC-SHA256 签名 → hex 字符串 */
    public static String hmacSign(SecretKey key, String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getEncoded(), "HmacSHA256"));
        byte[] result = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(result.length * 2);
        for (byte b : result) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }
}
