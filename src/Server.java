import java.net.*;
import java.security.*;
import java.security.cert.TrustAnchor;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.HashSet;
import java.util.Set;
import java.io.*;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Server for communication to Encrypto clients
 * 
 * @author Bradley Culligan, CLLBRA005
 * @version May 2022
 */

public class Server {
    private int port;
    // Store a set of unique connected usernames and rooms for the server to track
    private Set<String> usernames = new HashSet<>();
    private Set<Room> rooms = new HashSet<>();
    private X509Certificate trustAnchorCert;
    private X509Certificate serverCertificate;
    private KeyPair keyPair;

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
        if (!usernames.contains(username)) {
            usernames.add(username);
            return true;
        }
        return false;
    }

    /**
     * Attempts to remove a username from the server.
     * The method returns whether a user was successfully removed.
     */
    public synchronized boolean removeUser(String username) {
        return usernames.remove(username);
    }

    //Allows users to create rooms
    // Should check that the given room doesn't already exist
    public void addRoom(Room newRoom){
        rooms.add(newRoom);
    }

    //gets the room associated with the given ID. If this doesn't exist, it will return null. Might need handeling
    public Room getRoom(String roomID){
        Room returnRoom = null;
        for (Room r : rooms) {
            if (roomID.equals(r.getRoomID()))
                returnRoom = r;
        }
        return returnRoom;
    }

    private KeyPair serverKey() throws GeneralSecurityException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");

        kpGen.initialize(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4));

        return kpGen.generateKeyPair();
    }
    public static Date calculateDate(int hoursInFuture)
    {
        long secs = System.currentTimeMillis() / 1000;


        return new Date((secs + (hoursInFuture * 60 * 60)) * 1000);
    }
    public static synchronized BigInteger calculateSerialNumber(int serialNumberBase)
    {
        return BigInteger.valueOf(serialNumberBase++);
    }

    private X509Certificate createCACertificate(
            X509Certificate signerCert, PrivateKey signerKey,
            String sigAlg, PublicKey certKey, int followingCACerts)
            throws GeneralSecurityException,
            OperatorCreationException, CertIOException
    {
        X500Principal subject = new X500Principal("CN=Certificate Authority");


        X509v3CertificateBuilder certBldr = new JcaX509v3CertificateBuilder(
                signerCert.getSubjectX500Principal(),
                calculateSerialNumber(1),
                calculateDate(0),
                calculateDate(24 * 60),
                subject,
                certKey);


        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();


        certBldr.addExtension(Extension.basicConstraints,
                        true, new BasicConstraints(followingCACerts))
                .addExtension(Extension.keyUsage,
                        true, new KeyUsage(KeyUsage.keyCertSign
                                | KeyUsage.cRLSign));


        ContentSigner signer = new JcaContentSignerBuilder(sigAlg)
                .setProvider("BC").build(signerKey);


        JcaX509CertificateConverter converter = new JcaX509CertificateConverter().setProvider("BC");


        return converter.getCertificate(certBldr.build(signer));
    }
    public static X509Certificate createTrustAnchor(
            KeyPair keyPair, String sigAlg)
            throws OperatorCreationException, CertificateException
    {
        Security.addProvider(new BouncyCastleProvider());
        X500Name name = new X500Name("CN=Trust Anchor");


        X509v1CertificateBuilder certBldr = new JcaX509v1CertificateBuilder(
                name,
                calculateSerialNumber(0),
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
     * Starts server listening for connections
     */
    private void beginServer() {
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
     * Allows outputing messages to console for server logging
     */
    public void inform(String message) {
        System.out.println(message);
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
