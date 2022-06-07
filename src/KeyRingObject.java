import org.bouncycastle.jcajce.provider.asymmetric.X509;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

public class KeyRingObject implements Serializable {
    private String username;
    private X509Certificate userCertificate;

    /**
     * Constructor to connect user to server
     */
    public KeyRingObject(String username, X509Certificate userCertificate) throws GeneralSecurityException, IOException {
        this.username = username;
        this.userCertificate = userCertificate;
    }

    public X509Certificate getUserCertificate() {
        return userCertificate;
    }

    public String getUsername() {
        return username;
    }

    public PublicKey getPublicKey(){
        return userCertificate.getPublicKey();
    }

    public boolean matchAlias(String alias){
        if (this.username.equals(alias)){
            return true;
        }
        else
            return false;
    }
}
