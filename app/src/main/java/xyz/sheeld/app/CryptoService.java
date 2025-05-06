package xyz.sheeld.app;

import android.content.Context;
import android.util.Log;

import org.sol4k.Base58;
import org.sol4k.Connection;
import org.sol4k.Keypair;
import org.sol4k.PublicKey;
import org.sol4k.RpcUrl;
import org.sol4k.Transaction;
import org.sol4k.instruction.Instruction;
import org.sol4k.instruction.TransferInstruction;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class CryptoService {
    private static String DEPOSIT_WALLET_ADDRESS = "cs71CHU88LLHmHcWqL8pkDxtwLqQDvBFvzTnU6R3Hz4";
    private static long DEPOSIT_DEPOSIT_LAMPORT = 1000000;

    public static String signMessage(String privateKey) {
        byte[] secretKeyBytes = Base58.decode(privateKey);

        Keypair keypair = Keypair.fromSecretKey(secretKeyBytes);

        String message = "From Wallet";
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

        byte[] signature = keypair.sign(messageBytes);
        byte[] signatureOnly = Arrays.copyOfRange(signature, 0, 64);

        return Base58.encode(signatureOnly);
    }

    public static String sendTransaction(Context context) throws Exception {
        Connection connection = new Connection(RpcUrl.DEVNET);
        String blockHash = connection.getLatestBlockhash();

        Keypair sender = getKeypair(context);
        PublicKey senderPublicKey = new PublicKey(sender.getPublicKey().toString());
        PublicKey receiverPublicKey = new PublicKey(DEPOSIT_WALLET_ADDRESS);

        Instruction instruction = new TransferInstruction(senderPublicKey, receiverPublicKey, DEPOSIT_DEPOSIT_LAMPORT);
        Transaction transaction = new Transaction(blockHash, instruction, senderPublicKey);
        transaction.sign(sender);

        return connection.sendTransaction(transaction);
    }

    private static Keypair getKeypair(Context context) {
        Preferences pref = new Preferences(context);
        String pk = pref.getSolanaPrivateKey();
        byte[] privateKeyBytes = Base58.decode(pk);
        return Keypair.fromSecretKey(privateKeyBytes);
    }

    public static String getSolanaPublicKeyFromPrivateKey(String privateKey) {
        byte[] secretKeyBytes = Base58.decode(privateKey);

        Keypair keypair = Keypair.fromSecretKey(secretKeyBytes);

        return keypair.getPublicKey().toBase58();
    }
}
