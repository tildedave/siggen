import java.security.spec.X509EncodedKeySpec;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;

public class KeyTest {

    byte[] signature;
    byte data;

    public void test(byte[] bobEncodedPubKey) {
	X509EncodedKeySpec bobPubKeySpec = new X509EncodedKeySpec(bobEncodedPubKey);
	KeyFactory keyFactory = KeyFactory.getInstance("DSA");
	PublicKey bobPubKey = keyFactory.generatePublic(bobPubKeySpec);
	Signature sig = Signature.getInstance("DSA");
	sig.initVerify(bobPubKey);
	sig.update(data);
	sig.verify(signature);
    }
    
}