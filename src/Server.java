import java.net.*;
import java.security.*;
//import java.security.cert.TrustAnchor;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.HashSet;
import java.util.Set;
import java.io.*;
//import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
//import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
//import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.x500.X500Principal;

import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
//import java.util.Calendar;
import java.util.Date;

/**
 * Server for communication to Encrypto clients
 * 
 * @author Bradley Culligan, CLLBRA005
 * @author David Court, CRTDAV015
 * @author Michael Scott, SCTMIC015
 * @version June 2022
 */

public class Server {
    private int port;
    // Store a set of unique connected usernames and rooms for the server to track
    private ArrayList<String> usernames = new ArrayList<>();
    private Set<Room> rooms = new HashSet<>();
    private X509Certificate trustAnchorCert;
    private X509Certificate serverCertificate;
    private KeyPair keyPair;
    private ArrayList<X509Certificate> userCertificates = new ArrayList<X509Certificate>();
    static int count = 0;

    /**
     * Server constructor with port number supplied
     */
    public Server(int port) throws GeneralSecurityException, OperatorCreationException, CertIOException {
        this.port = port;
        this.keyPair = serverKey();
        KeyPair trustAnchorCertKey = serverKey();
        this.trustAnchorCert = createTrustAnchor(serverKey(), "SHA256WithRSA");
        this.serverCertificate = createCACertificate(trustAnchorCert, trustAnchorCertKey.getPrivate(), "SHA256WithRSA",
                keyPair.getPublic(), 0);
    }

    /**
     * Getter for server certificate
     */
    public X509Certificate getServerCertificate() {
        return serverCertificate;
    }

    /**
     * Check if room with ID supplied exists
     */
    public boolean containsRoom(String roomID) {
        for (Room r : rooms) {
            if (roomID.equals(r.getRoomID()))
                return true;
        }

        return false;
    }

    /**
     * Attempts to add a username to the server if it's available.
     * The method returns whether a user was successfully added.
     */
    public synchronized boolean addUser(String username) {
        return usernames.add(username);
    }

    /**
     * Attempts to remove a username from the server.
     * The method returns whether a user was successfully removed.
     */
    public synchronized boolean removeUser(String username) {
        return usernames.remove(username);
    }

    /**
     * Adds a new room to the set of rooms
     */
    public void addRoom(Room newRoom) {
        rooms.add(newRoom);
    }

    /**
     * Gets the room associated with the supplied room ID. If room doesn't exist,
     * this returns null
     */
    public Room getRoom(String roomID) {
        Room returnRoom = null;
        for (Room r : rooms) {
            if (roomID.equals(r.getRoomID()))
                returnRoom = r;
        }
        return returnRoom;
    }

    /**
     * Uses Bouncy Castle API to generate the key pair
     */
    private KeyPair serverKey() throws GeneralSecurityException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");

        kpGen.initialize(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4));

        return kpGen.generateKeyPair();
    }

    /**
     * Gets accurate date
     */
    public static Date calculateDate(int hoursInFuture) {
        long secs = System.currentTimeMillis() / 1000;

        return new Date((secs + (hoursInFuture * 60 * 60)) * 1000);
    }

    /**
     * Serial number counter
     */
    public static synchronized BigInteger calculateSerialNumber() {
        count += 1;
        return BigInteger.valueOf(count);
    }

    /**
     * Creates an X509 certificate and returns it
     */
    private X509Certificate createCACertificate(
            X509Certificate signerCert, PrivateKey signerKey,
            String sigAlg, PublicKey certKey, int followingCACerts)
            throws GeneralSecurityException,
            OperatorCreationException, CertIOException {
        X500Principal subject = new X500Principal("CN=Certificate Authority");

        X509v3CertificateBuilder certBldr = new JcaX509v3CertificateBuilder(
                signerCert.getSubjectX500Principal(),
                calculateSerialNumber(),
                calculateDate(0),
                calculateDate(24 * 60),
                subject,
                certKey);

        //JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();

        certBldr.addExtension(Extension.basicConstraints,
                true, new BasicConstraints(followingCACerts)).addExtension(Extension.keyUsage,
                        true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));

        ContentSigner signer = new JcaContentSignerBuilder(sigAlg)
                .setProvider("BC").build(signerKey);

        JcaX509CertificateConverter converter = new JcaX509CertificateConverter().setProvider("BC");

        return converter.getCertificate(certBldr.build(signer));
    }

    /**
     * Creates trust anchor certificate and returns it
     */
    public static X509Certificate createTrustAnchor(
            KeyPair keyPair, String sigAlg)
            throws OperatorCreationException, CertificateException {
        Security.addProvider(new BouncyCastleProvider());
        X500Name name = new X500Name("CN=Trust Anchor");

        X509v1CertificateBuilder certBldr = new JcaX509v1CertificateBuilder(
                name,
                calculateSerialNumber(),
                calculateDate(0),
                calculateDate(24 * 365),
                name,
                keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder(sigAlg)
                .setProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider()).build(keyPair.getPrivate());

        JcaX509CertificateConverter converter = new JcaX509CertificateConverter().setProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        return converter.getCertificate(certBldr.build(signer));
    }

    /**
     * Creates end entity certificate and returns it
     */
    public X509Certificate createEndEntity(String username,
            String sigAlg, PublicKey certKey)
            throws CertIOException, OperatorCreationException, CertificateException {
        X509Certificate signerCert = getServerCertificate();
        PrivateKey signerKey = keyPair.getPrivate();
        X500Principal subject = new X500Principal("CN=" + username);

        X509v3CertificateBuilder certBldr = new JcaX509v3CertificateBuilder(
                signerCert.getSubjectX500Principal(),
                calculateSerialNumber(),
                calculateDate(0),
                calculateDate(24 * 31),
                subject,
                certKey);

        certBldr.addExtension(Extension.basicConstraints,
                true, new BasicConstraints(false)).addExtension(Extension.keyUsage,true, new KeyUsage(KeyUsage.digitalSignature));

        ContentSigner signer = new JcaContentSignerBuilder(sigAlg).setProvider("BC").build(signerKey);

        JcaX509CertificateConverter converter = new JcaX509CertificateConverter().setProvider("BC");

        return converter.getCertificate(certBldr.build(signer));
    }

    /**
     * Stores the user certificate
     */
    public void addUserCertificates(String userName,
            String sigAlg, PublicKey certKey)
            throws CertIOException, OperatorCreationException, CertificateException {
        X509Certificate userCertificate = createEndEntity(userName, sigAlg, certKey);
        userCertificates.add(userCertificate);
    }

    public void addUserCertificate(X509Certificate userCertificate){
        userCertificates.add(userCertificate);
    }

    /**
     * Checks if the room ID's password hash, matches the password hash supplied when decrypted by the CA's private key
     */
    public boolean validRoomPassCombo(String roomID, String pass) {
        String decryptedPassToCheck = "";
        String decryptedRoomPass = getRoom(roomID).getPass(); // Currently stored as an encrypted hash
        
        try {
            decryptedPassToCheck = PGPUtil.decrypt(null, keyPair.getPrivate(), pass, 1); // Use CA private key to retrieve the hashed password to be checked
            decryptedRoomPass = PGPUtil.decrypt(null, keyPair.getPrivate(), decryptedRoomPass, 1); // Use CA private key to retrieve the hashed password of the room
        } catch (Exception e) {
            e.printStackTrace();
        }

        return decryptedPassToCheck.equals(decryptedRoomPass);

    }

    /**
     * Starts server listening for connections
     */
    private void beginServer() throws CertificateException, OperatorCreationException {
        try (ServerSocket ss = new ServerSocket(port)) {
            // Inform console that server successfully started
            System.out.println("Encrypto server started on port: " + port);

            // Accept client connections and serve forever
            while (true) {
                Socket socket = ss.accept();

                ServerThread serverThread = new ServerThread(socket, this);
                serverThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Allows outputing messages to console for server debug logging
     */
    public void inform(String message) {
        System.out.println("> " + message);
        System.out.println("");
    }

    public static void main(String[] args) throws GeneralSecurityException, OperatorCreationException, CertIOException {
        // Error checking for abnormal arguments
        if (args.length > 1) {
            System.err.println(
                    "Start server with no arguments to use default port (4444) else please supply one argument for the port number required.");
            System.exit(1);
        }

        // Wanted port defaults to 4444, check if user supplied port and construct
        // server
        int wantedPort = 4444;
        if (args.length == 1) {
            wantedPort = Integer.parseInt(args[0]);
        }

        Server server = new Server(wantedPort);
        server.beginServer();
    }
}
