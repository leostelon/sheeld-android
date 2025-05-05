package xyz.sheeld.app;

import android.util.Log;

import org.sol4k.Base58;
import org.sol4k.Keypair;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class CryptoService {
    public static String signMessage(String privateKey) {
        byte[] secretKeyBytes = Base58.decode(privateKey);

        Keypair keypair = Keypair.fromSecretKey(secretKeyBytes);

        String message = "From Wallet";
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

        byte[] signature = keypair.sign(messageBytes);
        byte[] signatureOnly = Arrays.copyOfRange(signature, 0, 64);

        return Base58.encode(signatureOnly);
    }

    public static String getSolanaPublicKeyFromPrivateKey(String privateKey) {
        byte[] secretKeyBytes = Base58.decode(privateKey);

        Keypair keypair = Keypair.fromSecretKey(secretKeyBytes);

        return keypair.getPublicKey().toBase58();
    }
}
