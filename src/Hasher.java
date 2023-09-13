import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hasher {
    public String encryptString(String message) throws NoSuchAlgorithmException{
        MessageDigest md=MessageDigest.getInstance("SHA-512");
        byte[] messageDigest = md.digest(message.getBytes());
        BigInteger bigInt = new BigInteger(1, messageDigest);
        return bigInt.toString();
    }

}