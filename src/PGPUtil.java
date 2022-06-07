//import org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi;
//import org.bouncycastle.openpgp.PGPPublicKey;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.Base64;

/**
 * PGPUtil for PGP implementation
 *
 * @author Bradley Culligan, CLLBRA005
 * @author David Court, CRTDAV015
 * @author Michael Scott, SCTMIC015
 * @version June 2022
 */

public class PGPUtil {

    /**
     * Sender side implementation of PGP
     * @param message
     * @param senderKeyPair
     * @param receiverPublicKey
     * @return
     * @throws Exception
     */
    public static String sender(String message, KeyPair senderKeyPair, PublicKey receiverPublicKey) throws Exception {
        // 1: Create a hash of the message
        String hashMessage = hashSHA(message);
        // System.out.println("Sender Hash: " + hashMessage);
        // 2: Digitally sign hash using private key
        String encryptedWithPriv = asymmetricEncrypt(senderKeyPair.getPublic(), senderKeyPair.getPrivate(), hashMessage, 0);
        //System.out.println(encryptedWithPriv);
        // 3: Append original message and encrypted hash
        String concatMessEncryptHash[] = {message, encryptedWithPriv};
        // 4: Compress 3
        String zipped[] = new String[concatMessEncryptHash.length];
        for (int i = 0; i < concatMessEncryptHash.length ; i++) {
            zipped[i] = compress(concatMessEncryptHash[i]);
        }
        // 5: Encrypt zipped with AES
        SecretKey sharedKey = KeyGenerator.getInstance("AES").generateKey();
        String zippedAES[] = new String[zipped.length + 1];
        for (int i = 0; i < zipped.length; i ++){
            zippedAES[i] = encryptAES(zipped[i], sharedKey);
        }

        // 6: Encrypt shared key with public key using RSA
        String encodedSharedKey = Base64.getEncoder().encodeToString(sharedKey.getEncoded());
        String encryptSharedWithPublic = asymmetricEncrypt(receiverPublicKey, null, encodedSharedKey, 1);

        // 7: Append 5 and 6 and send as final message
        zippedAES[2] = encryptSharedWithPublic;
        String returnMessage[] = zippedAES;

        String returnMessageOut = "";
        for (int i = 0; i < returnMessage.length; i ++){
            if (i <= 1) {
                returnMessageOut += returnMessage[i] + ";";
            }
            else
                returnMessageOut += returnMessage[i];
            System.out.println(i + " : " + returnMessage[i]);
        }

        System.out.println("Done Sender");
        return returnMessageOut;
    }

    /**
     * Receiver side implementation of PGP
     * @param returnMessageOut
     * @param senderpubKey
     * @param receiverPublicKey
     * @param receiverPrivateKey
     * @return
     * @throws Exception
     */
    public static String receiver(String returnMessageOut, PublicKey senderpubKey, PublicKey receiverPublicKey, PrivateKey receiverPrivateKey) throws Exception {
        // 1: Decrypt secret key of AES using private key
        String[] returnMessage = returnMessageOut.split(";", 3);
        String receivedEncodedSecretKey = decrypt(receiverPublicKey, receiverPrivateKey, returnMessage[2], 1);
        byte[] receivedDecodedSecretKey = Base64.getDecoder().decode(receivedEncodedSecretKey);
        SecretKey actualKey = new SecretKeySpec(receivedDecodedSecretKey, 0, receivedDecodedSecretKey.length, "AES");

        // 2: Decrypt Rest of message in returnMessage with actualKey
        String receiveDecryptedMessage[] = new String[returnMessage.length - 1];
        for (int i = 0; i < returnMessage.length-1; i++) {
            returnMessage[i] = decryptAES(returnMessage[i], actualKey);
        }
        // 3: Unzip this message
        String unzipstring[] = new String[receiveDecryptedMessage.length];
        for (int i = 0; i < unzipstring.length; i++) {
            unzipstring[i] = decompress(returnMessage[i]);
        }
        String decryptedMessage = unzipstring[0];
        System.out.println("Decrypted Message: " + decryptedMessage);

        // 4: Verify Digital signature
        String receivedhash = decrypt(senderpubKey, null, unzipstring[1], 0);
        System.out.println("Received Hash: " + receivedhash);
        String calculateHash = hashSHA(decryptedMessage);
        System.out.println("Calculated Hash: " + calculateHash);
        if (receivedhash.equals(calculateHash)){
            System.out.println("Received Hash and Calculated Hash are equal.");
            return "Unverified Sender";
        }
        else
            System.out.println("Oops there is a hacker");
            return decryptedMessage;
        }

    /**
     * Returns the hash of a given string using the SHA-512 algorithm
     */
    public static String hashSHA(String message) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        String out = "";
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        digest.reset();
        digest.update(message.getBytes("utf8"));
        out = String.format("%040x", new BigInteger(1, digest.digest()));
        return out;
    }

    /**
     * For a given key pair and message, encrypt and return the message using either the public or private key
     * depending on the mode chosen. Encryption uses the RSA algorithm in ECB mode with PKCS1Padding.
     */
    public static String asymmetricEncrypt(PublicKey publicKey, PrivateKey privateKey, String message, int mode) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        if (mode == 0){
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            byte[] utf8 = cipher.doFinal(message.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(utf8);   // Probably where the problem will be
        }
        else {
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] utf8 = cipher.doFinal(message.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(utf8);
        }
    }

    /**
     * For a given key pair and encrypted message, decrypt and return the message using either the public or private key
     * depending on the mode chosen. Decryption uses the RSA algorithm.
     */
    public static String decrypt(PublicKey publicKey,PrivateKey privateKey, String st, int ch) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        byte[] encrypted = Base64.getDecoder().decode(st);
        if (ch == 0) {
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            byte[] utf8 = cipher.doFinal(encrypted);
            return new String(utf8, "UTF8");
        }
        else {
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] utf8 = cipher.doFinal(encrypted);
            return new String(utf8, "UTF8");
        }
    }

    /**
     * Compresses a given message using gzip compression
     */
    public static String compress(String data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length());
        GZIPOutputStream gzip = new GZIPOutputStream(bos);
        gzip.write(data.getBytes());
        gzip.close();
        byte[] compressed = bos.toByteArray();
        bos.close();
        return Base64.getEncoder().encodeToString(compressed);
    }

    /**
     * Encrypts a message with a given AES generated shared key. 
     */
    public static String encryptAES(String str, SecretKey key) throws Exception {
        Cipher ecipher = Cipher.getInstance("AES");
        ecipher.init(Cipher.ENCRYPT_MODE, key);
        // Encode the string into bytes using utf-8
        byte[] utf8 = str.getBytes("UTF8");
        // Encrypt
        byte[] enc = ecipher.doFinal(utf8);
        // Encode bytes to base64 to get a string
        return Base64.getEncoder().encodeToString(enc);
    }

    /**
     * Decrypts some ciphertext with a given AES generated shared key. 
     */
    public static String decryptAES(String st, SecretKey key) throws Exception {
        Cipher dcipher = Cipher.getInstance("AES");
        dcipher.init(Cipher.DECRYPT_MODE, key);
        // Decode base64 to get bytes
        byte[] dec = Base64.getDecoder().decode(st);
        byte[] utf8 = dcipher.doFinal(dec);
        // Decode using utf-8
        return new String(utf8, "UTF8");
    }

    /**
     * Unzips/decompresses a compressed message
     */
    public static String decompress(String st) throws IOException {
        byte[] compressed = Base64.getDecoder().decode(st);
        ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
        GZIPInputStream gis = new GZIPInputStream(bis);
        BufferedReader br = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        gis.close();
        bis.close();
        return sb.toString();
    }

    /**
     * Main method to test entire PGP protocol. Not used by application.
     */
    public static void main (String[] args){
        try{
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

            KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");

            kpGen.initialize(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4));

            KeyPair keyPairSender = kpGen.generateKeyPair();

            KeyPairGenerator kpGen2 = KeyPairGenerator.getInstance("RSA", "BC");

            kpGen2.initialize(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4));

            KeyPair keyPairReceiver = kpGen.generateKeyPair();

            String input = "We are building the encrypto app";
            System.out.println("Sent input: " + input);

            PublicKey pub1 = keyPairReceiver.getPublic();
            System.out.println("Pub1: " + pub1);

            String encodedPubKey = Base64.getEncoder().encodeToString(pub1.toString().getBytes("UTF-8"));
            System.out.println("Pub1 encoded: " + encodedPubKey);

            byte[] receivedDecodedSecretKey = Base64.getDecoder().decode(encodedPubKey);
            String decodedPubKey = new String(receivedDecodedSecretKey, "UTF-8");
            System.out.println("Decoded key: " + decodedPubKey);

            SecretKey actualKey = new SecretKeySpec(receivedDecodedSecretKey, 0, receivedDecodedSecretKey.length, "AES");

            //String fromSender = sender(input, keyPairSender, keyPairReceiver.getPublic());
            //System.out.println(fromSender);
            //receiver(fromSender, keyPairSender.getPublic(), keyPairReceiver.getPublic(), keyPairReceiver.getPrivate());
        }
        catch(Exception e){
            e.printStackTrace();
        }
        
    }
}
