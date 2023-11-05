package cn.cnic.dataspace.api.util;

import org.apache.commons.codec.binary.Base64;
import javax.crypto.Cipher;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RSAEncrypt {

    // Public key
    private static String publicKey = "";

    // Private key
    private static String privateKey = "";
    /**
     * Randomly generate key pairs
     */
    public static void genKeyPair() throws NoSuchAlgorithmException {
        // The KeyPairGenerator class is used to generate public and private key pairs, and generates objects based on the RSA algorithm
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        // Initialize the key pair generator with a key size of 96-1024 bits
        keyPairGen.initialize(1024, new SecureRandom());
        // Generate a keypair and save it in keyPair
        KeyPair keyPair = keyPairGen.generateKeyPair();
        // Obtain private key
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        // Obtain public key
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        String publicKeyString = new String(Base64.encodeBase64(publicKey.getEncoded()));
        // Get private key string
        String privateKeyString = new String(Base64.encodeBase64((privateKey.getEncoded())));
        // Save public and private keys to Map
        System.out.println(publicKeyString);
        System.out.println(privateKeyString);
    }

    /**
     * RSA Public Key Encryption
     */
    public static String encrypt(String str) {
        try {
            // Base64 encoded public key
            byte[] decoded = Base64.decodeBase64(publicKey);
            RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
            // RSA encryption
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            String outStr = Base64.encodeBase64String(cipher.doFinal(str.getBytes("UTF-8")));
            return outStr;
        } catch (Exception e) {
            // e.printStackTrace();
            return null;
        }
    }

    /**
     * RSA private key decryption
     */
    public static String decrypt(String str) {
        try {
            // 64 bit decoded encrypted string
            byte[] inputByte = Base64.decodeBase64(str.getBytes("UTF-8"));
            // Base64 encoded private key
            byte[] decoded = Base64.decodeBase64(privateKey);
            RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
            // RSA decryption
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, priKey);
            return new String(cipher.doFinal(inputByte));
        } catch (Exception e) {
            // e.printStackTrace();
            return null;
        }
    }
}
