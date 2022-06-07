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
import java.util.Arrays;
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
     * 
     * @param message
     * @param senderKeyPair
     * @param receiverPublicKey
     * @return
     * @throws Exception
     */
    public static String sender(String message, KeyPair senderKeyPair, PublicKey receiverPublicKey) throws Exception {
        // 1: Create a hash of the message
        System.out.println("Print statements for process of encrypting message");
        String hashMessage = hashSHA(message);
        System.out.println("Sender Hash: " + hashMessage);
        // 2: Digitally sign hash using private key
        String encryptedWithPriv = asymmetricEncrypt(senderKeyPair.getPublic(), senderKeyPair.getPrivate(), hashMessage,
                0);
        System.out.println("Hashed Message encrypted with private key: " + encryptedWithPriv);
        // 3: Append original message and encrypted hash
        String concatMessEncryptHash[] = { message, encryptedWithPriv };
        // 4: Compress 3
        String zipped[] = new String[concatMessEncryptHash.length];
        for (int i = 0; i < concatMessEncryptHash.length; i++) {
            zipped[i] = compress(concatMessEncryptHash[i]);
        }
        System.out.println("Everything now zipped");
        // 5: Encrypt zipped with AES
        SecretKey sharedKey = KeyGenerator.getInstance("AES").generateKey();
        System.out.println("Generated Secret/Shared key:" + sharedKey.toString());
        String zippedAES[] = new String[zipped.length + 1];
        for (int i = 0; i < zipped.length; i++) {
            zippedAES[i] = encryptAES(zipped[i], sharedKey);
        }

        // 6: Encrypt shared key with public key using RSA
        String encodedSharedKey = Base64.getEncoder().encodeToString(sharedKey.getEncoded());
        String encryptSharedWithPublic = asymmetricEncrypt(receiverPublicKey, null, encodedSharedKey, 1);
        System.out.println("Encrypted and encoded shared key: " + encryptSharedWithPublic);
        // 7: Append 5 and 6 and send as final message
        zippedAES[2] = encryptSharedWithPublic;
        String returnMessage[] = zippedAES;

        String returnMessageOut = "";
        for (int i = 0; i < returnMessage.length; i++) {
            if (i <= 1) {
                returnMessageOut += returnMessage[i] + ";";
            } else
                returnMessageOut += returnMessage[i];
            //System.out.println(i + " : " + returnMessage[i]);
        }
        System.out.println("Finally Concatenated message to receiver: " + returnMessageOut);
        System.out.println("Done Sender");
        System.out.println("");
        return returnMessageOut;
    }

    /**
     * Receiver side implementation of PGP
     * 
     * @param returnMessageOut
     * @param senderpubKey
     * @param receiverPublicKey
     * @param receiverPrivateKey
     * @return
     * @throws Exception
     */
    public static String receiver(String returnMessageOut, PublicKey senderPubKey, PublicKey receiverPublicKey,
            PrivateKey receiverPrivateKey) throws Exception {
        // 1: Decrypt secret key of AES using private key
        System.out.println("");
        String[] returnMessage = returnMessageOut.split(";", 3);
        String receivedEncodedSecretKey = decrypt(receiverPublicKey, receiverPrivateKey, returnMessage[2], 1);
        byte[] receivedDecodedSecretKey = Base64.getDecoder().decode(receivedEncodedSecretKey);
        SecretKey actualKey = new SecretKeySpec(receivedDecodedSecretKey, 0, receivedDecodedSecretKey.length, "AES");
        System.out.println("Print statements for process of unencrypting message");
        System.out.println("Unencrypted secret/session key" + actualKey.toString());

        // 2: Decrypt Rest of message in returnMessage with actualKey
        String receiveDecryptedMessage[] = new String[returnMessage.length - 1];
        for (int i = 0; i < returnMessage.length - 1; i++) {
            returnMessage[i] = decryptAES(returnMessage[i], actualKey);
        }
        // 3: Unzip this message
        String unzipstring[] = new String[receiveDecryptedMessage.length];
        for (int i = 0; i < unzipstring.length; i++) {
            unzipstring[i] = decompress(returnMessage[i]);
        }
        String decryptedMessage = unzipstring[0];
        System.out.print("Unencrypted and unzipped message: " + decryptedMessage);
        // System.out.println("Decrypted Message: " + decryptedMessage);

        // 4: Verify Digital signature
        String receivedhash = decrypt(senderPubKey, null, unzipstring[1], 0);
        System.out.println("Received Hash: " + receivedhash);
        String calculateHash = hashSHA(decryptedMessage);
        System.out.println("Calculated Hash: " + calculateHash);
        if (receivedhash.equals(calculateHash)) {
            System.out.println(
                    "Received Hash and Calculated Hash are equal therefore authentication and confidentiality achieved");
            System.out.println("Done Receiver");
            System.out.println();
            return decryptedMessage;
        } else
            System.out.println("Oops there is a hacker");
        return "Unverified Sender";
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
     * For a given key pair and message, encrypt and return the message using either
     * the public or private key
     * depending on the mode chosen. Encryption uses the RSA algorithm in ECB mode
     * with PKCS1Padding.
     */
    public static String asymmetricEncrypt(PublicKey publicKey, PrivateKey privateKey, String message, int mode)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException,
            IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        if (mode == 0) {
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            byte[] utf8 = cipher.doFinal(message.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(utf8); // Probably where the problem will be
        } else {
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] utf8 = cipher.doFinal(message.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(utf8);
        }
    }

    /**
     * For a given key pair and encrypted message, decrypt and return the message
     * using either the public or private key
     * depending on the mode chosen. Decryption uses the RSA algorithm.
     */
    public static String decrypt(PublicKey publicKey, PrivateKey privateKey, String st, int ch) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        byte[] encrypted = Base64.getDecoder().decode(st);
        if (ch == 0) {
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            byte[] utf8 = cipher.doFinal(encrypted);
            return new String(utf8, "UTF8");
        } else {
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
        while ((line = br.readLine()) != null) {
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
    public static void main(String[] args) {
       /*try {
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

             String input = "Hellooooooo";
             System.out.println("Sent input: " + input);

             PublicKey pub1 = keyPairReceiver.getPublic();
             System.out.println("Pub1: " + pub1);
             String encodedPubKey = Base64.getEncoder().encodeToString(pub1.getEncoded());
             System.out.println("Pub1 encoded: " + encodedPubKey);
             byte[] receivedDecodedSecretKey = Base64.getDecoder().decode(encodedPubKey);
             String decodedPubKey = receivedDecodedSecretKey.toString();
             System.out.println("Decoded key: " + decodedPubKey);
             SecretKey actualKey = new SecretKeySpec(receivedDecodedSecretKey, 0,
             receivedDecodedSecretKey.length, "AES");

             String encodedPubKey =
             Base64.getEncoder().encodeToString(pub1.toString().getBytes("UTF-8"));
             System.out.println("Pub1 encoded: " + encodedPubKey);

             byte[] receivedDecodedSecretKey = Base64.getDecoder().decode(encodedPubKey);
             String decodedPubKey = new String(receivedDecodedSecretKey, "UTF-8");
             System.out.println("Decoded key: " + decodedPubKey);

             SecretKey actualKey = new SecretKeySpec(receivedDecodedSecretKey, 0,
             receivedDecodedSecretKey.length, "AES");


             String fromSender = sender(input, keyPairSender,
             keyPairReceiver.getPublic());
             System.out.println(fromSender);
             receiver(fromSender, keyPairSender.getPublic(), keyPairReceiver.getPublic(),
             keyPairReceiver.getPrivate());

              }
              catch(Exception e){
              e.printStackTrace();
              }

              String fromSender = sender(input, keyPairSender,
              keyPairReceiver.getPublic());

              System.out.println("The encrypted message from sender: " + fromSender);
              System.out.println();

              String received = receiver(fromSender, keyPairSender.getPublic(),
              keyPairReceiver.getPublic(), keyPairReceiver.getPrivate());
              System.out.println(received);

            // String test =
            // "Hi>Mike1<I1Pj4i2YABtP5EbPjS1LNDz9M54qp3uXKxQKRvH1H3K6pQrcoLAmESp9j8rIeCD9wKCVEAE4i0tv85S9xZImpA==;SI5fuv7mmT0275BzXssimSB6ELdQ0NVLGc65BhmoR75lpCAY6hxqfZbJr/m3241Vn9z9+qqjrJ8f6APgLRfn8ji/B97nOMwjIAW+k7x1ma0gd5OfbCFxGCvGClkVDRhQvFLc/CBj8LyvM/CxR4J40MdI5brFLppvOUJZuV6cwK5mGrEAWR8OsvoLbhZ47/VCs4PE2QzNpUT/1sd9T8Ey2VpEA3wt8R2DbTrCz9VhtGU0gkygWI/GIXnURzJd1pc3YpYPrm8xvS1UkQ5nASx71HwrO/k8JfXzhfj+D7U7SJXePHU+YCnbeCMQyd8GKIooGaZGLf1qDZq+QuS8Ip+BKhaM4aBN++u82O4WzDutv09Z4W205vTSW2BZpUqxscvJS+n14ZCSJ0vumS9sDIKUF/CljywNZrGjTMqWKgIOwxzh778ZVx4DJYpMGxiu6bdWHdH92SLhoq4u4O4Icz7R8oGtjZfFj6M8CZhjD5sNKoLXEGVrnWmdltggMGWA0wwdGacKThCR0apH7W6gLu0JobiWyyx4zYTjR07ndT+YGbg=;COmt0HfAv0qJfOmi8YqtSbpfg+/+y02KzfZdB0kicqcsoiNCkqisLYNCjE09FNmiTZ1+6a4T9Cq7MYRwtC2wYhxqNvSUokqeb5tF3S1ouCRYAsa9aUlay86Gvq4JdS7yJuQ4JQohlHwZuOJwQ06T7fqNulVvEdv85IHMewRF/dAEQep1yrLGYy2iSrSWhUy/La1ls/Deuyf8owsaC/6WlUh2IeCbasOer66WbdzjwZHNDz1cakczsJCedfcpdiBBHAMb4OjRfBbs3/9JfxtAmExZKkhcNQp5dYi6Mal0sWaO5c1Hj3IctnzQUyFaCGxbZyv3SdrykcvlBspnTrzA3w==>Mike2<A7NjwTvCGWzdWc7AHuFJgBdYyGvRken1JW4R+i9N46jkpNtAHR+c3ZvDrUZZ+PFy1Ay9IRa3/GLoXWe/d6jM5Q==;yVytbDUHGYc0k5cW4x9dLW4LNV/jUzrT/71Cpezq50kFAQoxjvSH8ADt29LLLiKmsjKodrap9sD1z/4ljxhlpB7ToEben2pcA3CUwM12o8lTxVel1cbkjOXSrwZw2sbAKueFu3eFMlxYyIC/WBNhSsBNSA892rudafcQUvIiVWr+th3SC69vDmtUFwgl/QTUU/vfqK8zd81AJdvvg+wb9wKfub1r5EYeJevVCRdTBXgZ1bkv1dLpfpb/9kaqRw5m7JXf8+uhWOKda1uWKFQluKWpcigpJelCsIGl2T6fmVdPHtcDbnQw3wmGYmsya3dhQ79bZwvUoC7houL/PG5BsuNltAGR88pDKrszV2/DDE9wMLF/RQw/v2jjH32mYPcnMYIJUNvBlhX0i0/7ch9X7lThONfEzfYdBgoX0A4cB25Nm0U2VYhgvCLJPoOaSLecw/aaq7xF/YOH90zN8I8jQNB7EEvzaZXz4+JdHBR6SMUgPr7p7u8Bu1ClVvvB3jCXNYIIHejV9GPfG4VdXOcQP4vdEn9r0xiWsXrFFWquswU=;GVDKm+xcGX8y7qkgUqyt4fbzJYM4K19CP4pYEsvTbeOR8n1bxHNs5k1PXtWlIIQpdHB+OcmsS71JtH/6jUQhocw3H1XsxH0IkigF22UH8630m1vC5uBHd8pRI34gKxd/mA2C3ofzQwZqWgzwvb3pRlboW1ZYgL8LQeTIOjao3ik/i6uaegJ0tvJYubbTnKGSBqU/eA5eWDmhVe15wXE9WVg3jkBFN4Aif+yAjK2opwCPVrqmBEnMyOXREersutFNy3bcZYY5jW4M+D5SMzZ1k8o9SF6USf9/kefdEcaVBubGgpK570ERkCZgB4CZYphqLrfbWwyvK3wvglrHuVKoDg==>Mike3<KywyRVoK6ft8pow+YJmRYwJndfE0Vh0cD8QDrjlGLUeQH2vUYl5jEeJ2cMpo9B/232i1+d9HBa3TDngQhYP+aQ==;thcIq8dKt9wwa6InqNNgQ6f1BbTNwDt0LR+gbFHYg5s9KDXG0RpSc/4njttaGA3/jKLrqH+VmR+XcN4jxY1xtGplEmT02L87G189okFYaOaWlOoYxxUo/LvOaZAK7NAq/VXmLkPC0pm9fVyiTvmPUIxkGQ/19ZT24tdMCL37DQKjclgEGO0NQM4PStSNS3xzE4eqN4jYEO190wu2BkDLo2hfCG/6OIO/Y5Uy1ll5344rGEcj3Nc8Masr59masRo4+lDg94unIlO63k1Ki7hkmNDjoDHVg6QYx4SeUzoWpnhQPmTDRMQhN4uKQB+3l+TGSFeHgDPoDwyYbGTcJKQ4IfTt5KnL1or1KrEMA9lc3Y8xWTGLsV5e0VOoB+LYelIfKBZCreIPjpZwHx5ohOvXI5eQFZZIDZbuhY1wipgRCopFkOjRddyYtemaY+UnACHGGWDcSPLNDZlkSg61i5FUW3zhj+HiLJFSsjL40M0m5ODaGAqAH4vnsr/cEYu1P0MzOxl/G+jZV9XJ9t2rbNYdGLefDMUSLv6t/dv5dzigXW0=;Gn1Gc7D7VKamdbXbEyT4MGwAI0YQCGPnLjlBirtvPVgN6qzEArzoh9gl1hNgY7iiAfbOLAkkTxK9jFw7fUKk1pQK2diShWFo+h8IysmUMaB0PXGtDzX3AMvthzHBg5fe3r97r0IlIcW+mGoR3gijjw5F0zCjr5HrsRIs4svBzbXLavewRO8cPUyUURSkUgAzrejvaaT6yZQpb0qdW+1qqaTuf0rjH3qK0UsdvVnSnnQr8Nzj7Ecn7e48qCgqAQKuREjF2lYIr7DG+/g4JijqjsuQY97yQEETYibGWB9RaMm6460B7k7ZyD4mw7b+X6ZwMlpP1dUDTSVJW40p9J0AnA==";
            String test = ":MESSAGE:[Mike2] Yo>Mike<tRLSgZFvDm7WBvho6MPR+IQutMAPPSh2FE3FhOcCS2zV+3GL+mDgPJI+hRQt78/lLi+m1psHjgGDRvhI0Y7AGw==;f+tnFdcnmlHwC25//u5HzXMxEhWhTHbzeqwhSg2/RgmMNEoeH5vmtgOpI8POf6fEbcny2+eb8EFaNEzcttbXzlVtYTRUDrhyIL/KO5DjLeY3TVe+e/ph/n2wWSeUoxNGo04W6rLVspEnohwuyuRSX1xBFxkRDiy+Y7NsGT5et7yZNrrsmOl3k5ZSsJ00995qibsu1KmBZoswrl9KPUFCByFG2kX4DncWV7spOcxqiLWFYuKE23znqdD3E6pKx2BP3IxdhoqDN7Zi2LTAQGcAOjuvq8ZXI3tLIAn+7SccroYOR2nsigiTuI+xEVIUE6/3ltm/j9SQel2DDSDNDpqdB/qwuA2RLMeLemWwKYhNnJ5n5Khti9CisUSpvQx2b9vlvZb6KjoiJu23g4wafDvimAL0bRbhSFyzp+S5Wx0enAuqCMzxkwgbZrYzGpjoF6ZI4RmEFFUHudMCXYY0evcO7poSGLBsRbisjZVQ+ArIVf6l3BDsBKOv+25XNAUG/2uRKyikaIobOl4lH8HZKA4U4aOUo1lhVQUqH6KGe5u6kwc=;gfmWQxvcz9iyY1ZGoSIDZNJhcX2S/A0/xol87+sn5IrNx4Ifoes1FhtwkuOAkSowdMPKCKzqjnjZQVEvcdFwmr8Z5XUyPVe1weMDCjYkhdtByQh4yLyEWLUo3TzQA3WzK243D1oimyQSj0Re1BuLTUwpedjR/RjPTa0VJPUbusw+qfyja2pr5ZKGJPE4mAgzyCTWRVrfGtmdCM+aSyCor6Kw+HbivzNsPnE32aBGnBphOtryES7GFJ90fsuLnukJgfTbUwsokR2Llx0cHmPv9zTmghbVpncn2kKB0JTy8DH5Mb8XNJFkDtsmPvC2rJkjX1Yv3gwzZA7A9MU6Q1Y5eA==>Mike2<aKtMms9Zstzp+8CIc4cEouNba/hrdM8WGR9moHbv0FW4/F7FMbgCbFtxz5wA/N34utwkBRsWXdmgtU8nYeW0jg==;QUd55d7NLMVaURS4G+BmuKPSBe6vZHfcJ/FwZcJVnDP9uwKN1kPi//MCzASAnCvLkwx4CZudhTDHCBAn5tf/vmDppxMbA3HJQrCIG/q8u3ZiK2cgTG7B39NIRToB/A7EId04i3L8Iyk/FXnd8SjQFpu77+Nq+3dA/tNF6R4Bw4YspHV82CBDX862kKT2W6vZ6J2G8N5/WOS6PKGVyZ7AjNpLSpjajhzh8aHSBNQ44hJF4BfgztT8Nc1tzQBGr1nLvheFLvWvP2EQoIJZl2Q6o9oJaPq3pB/duB2mxwJ7V819RqpLkU/ASHbAyl7QQ5in51lhKVn+95Ifcc81bms3JF2k7NIhBgyokTIElMTdfYSG6bxGPR4KpTeUZwVrs/y7p7J2M5SnleIcEYoraWV0VcRinG1GFLuG1CyRDEU6KUCxGXOWfE0Q+Sw0l1ZfPpOLA7vlQqDzjGaDDhFbcVWF0LZnJHJk1PTfCy1VkvLsDcrsyJjRXPZw8cIl1eHJIMvntdCfiNPM9ggZ3BIjIDz4M/3TQpqBVMeyIgySueRvc9Q=;Atscvv/+vnEolmcxfuEBPyk1UxcBm5d0vrY/CmU+StQTlPdSWZV4xZnqzPZLckVLK+oKEPuy7HY6veX2P8qs6mgt1K/FXW9zUWQZoiNnRZrsG2yOsMv+WFb2bqtiF5rc5GobpcUmms4D331OnAYs8uSQ6Zb8KTeBhwI0cqRfAy5dii/wXtaL73y1H1hcj6juBj4NqVQy4zNEBqE8Hvlda689hlYMTZ5iZFNw3gPdUjwA/HX5l3ZIJUpvEjGgx4LnvjYwQc/gYcllSexk0s2sFAFGd6qFRUIy+mqj6xga9ai+dTRJJI0gbtaRDBAqqQcyoPhHmFnzojkRgu0VLcZSYA=";
            String[] test2 = test.split("]");
            String Header = test2[0];
            String initialMessage = test2[1].trim();
            String[] initialMessageBroken = initialMessage.split(">");
            for (int i = 0; i < initialMessageBroken.length; i++) {
                String message = initialMessageBroken[i];
                System.out.println(message);
                String[] messageSplit = message.split("<");
                System.out.println(Arrays.toString(messageSplit));
            }
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } */
    }
}
