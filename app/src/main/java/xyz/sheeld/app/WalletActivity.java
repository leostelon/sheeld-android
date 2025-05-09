package xyz.sheeld.app;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.ViewCompat;

import com.google.gson.Gson;

import org.sol4k.Base58;
import org.sol4k.Keypair;

import java.util.List;
import java.util.Objects;

import xyz.sheeld.app.api.controllers.ClientController;
import xyz.sheeld.app.api.controllers.NetworkController;
import xyz.sheeld.app.api.controllers.WalletController;
import xyz.sheeld.app.api.dtos.PostWalletOverviewResponseDTO;
import xyz.sheeld.app.api.interfaces.DataCallbackInterface;
import xyz.sheeld.app.api.types.Node;
import xyz.sheeld.app.vpn.VPN;

public class WalletActivity extends AppCompatActivity {
    private Preferences prefs;
    private final WalletController walletController = new WalletController();
    private static final String defaultPrivateKey = "53ESETwLEZbKFYtkC3G7qFGzFaTaTWXApU18EWXecVszdMbSDcbfyjxXYZ1fM45gEMMr9zJviN25GVFZV9htrshK";
    private TextView balance;
    private boolean planExpired = true;
    private TextView upgradeButtonTitle;
    private LinearLayout walletContainer;
    private final ClientController clientController = new ClientController();
    private final NetworkController networkController = new NetworkController();
    private List<Node> bootNodes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        prefs = new Preferences(this);

        // Set ActionBar title
        if (getSupportActionBar() != null) {
            GradientDrawable actionBarBackground = new GradientDrawable();
            actionBarBackground.setColor(Color.WHITE);
            getSupportActionBar().setBackgroundDrawable(actionBarBackground);
            SpannableString s = new SpannableString("Wallet");
            s.setSpan(new ForegroundColorSpan(Color.BLACK), 0, s.length(), 0); // Change to desired color
            getSupportActionBar().setTitle(s);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            final Drawable upArrow = AppCompatResources.getDrawable(this, R.drawable.back);
            getSupportActionBar().setHomeAsUpIndicator(upArrow);
        }

        Context  context = getApplicationContext();
        LinearLayout linearLayout = new LinearLayout(context);
        ViewCompat.setOnApplyWindowInsetsListener(linearLayout,(v, insets) -> {
            int topInset = insets.getSystemWindowInsetTop();
            int bottomInset = insets.getSystemWindowInsetBottom();
            linearLayout.setPadding(32, topInset, 32, bottomInset);
            return insets;
        });
        linearLayout.setBackgroundColor(Color.WHITE);

        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(Gravity.TOP);

        String privateKey = prefs.getSolanaPrivateKey();
        walletContainer = new LinearLayout(context);
        linearLayout.addView(walletContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        walletContainer.setVisibility(Objects.equals(privateKey, defaultPrivateKey) ? View.GONE : View.VISIBLE);

        // Wallet Address
        LinearLayout walletAddressContainer = new LinearLayout(context);
        walletContainer.addView(walletAddressContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 12, 0, 12));
        walletAddressContainer.setGravity(Gravity.CENTER);
        walletContainer.setOrientation(LinearLayout.VERTICAL);

        String wa = prefs.getSocksUsername();
        String formattedWalletAddress = wa.substring(0, 5)+ "..." + wa.substring(wa.length() - 5);
        TextView walletAddress = new TextView(context);
        walletAddressContainer.addView(walletAddress);
        walletAddress.setText(formattedWalletAddress);

        ImageView copyIcon = new ImageView(context);
        walletAddressContainer.addView(copyIcon, LayoutHelper.createLinear(50, 50));
        copyIcon.setImageResource(R.drawable.copy);
        copyIcon.setOnClickListener(view -> AndroidUtilities.copyToClipboard(context, wa));

        // Balance
        balance = new TextView(context);
        walletContainer.addView(balance, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        balance.setText("0SOL");
        balance.setTypeface(AndroidUtilities.getBoldTypeface(context));
        balance.setGravity(Gravity.CENTER);
        balance.setTextSize(32);

        GradientDrawable upgradeButtonBackground = new GradientDrawable();
        upgradeButtonBackground.setCornerRadius(12);
        upgradeButtonBackground.setColor(getResources().getColor(R.color.primary));

        LinearLayout upgradeButton = new LinearLayout(context);
        walletContainer.addView(upgradeButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 12, 0, 12));
        upgradeButton.setBackground(upgradeButtonBackground);
        upgradeButton.setPadding(12, 12, 12, 12);
        upgradeButton.setOnClickListener(view -> upgradeHandler());

        upgradeButtonTitle = new TextView(context);
        upgradeButton.addView(upgradeButtonTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        upgradeButtonTitle.setGravity(Gravity.CENTER);
        upgradeButtonTitle.setText("Upgrade");
        upgradeButtonTitle.setTextColor(Color.WHITE);

        // Payment description
        TextView note = new TextView(context);
        walletContainer.addView(note);
        note.setText("Note:");
        note.setTypeface(AndroidUtilities.getMediumTypeface(context));
        note.setTextColor(Color.BLACK);

        TextView noteSub = new TextView(context);
        walletContainer.addView(noteSub);
        noteSub.setText("Transaction takes place on Devnet and Pro plan costs 0.001SOL");
        noteSub.setTypeface(AndroidUtilities.getRegularTypeface(context));
        noteSub.setTextColor(Color.GRAY);

        // Section Divider
        View sectionDivider = new View(context);
        sectionDivider.setBackgroundColor(getResources().getColor(R.color.border));
        walletContainer.addView(sectionDivider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1,0, 24, 0, 0));

        // Import
        TextView importTitle = new TextView(context);
        linearLayout.addView(importTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 24, 0, 0));
        importTitle.setText("Import Wallet");
        importTitle.setTypeface(AndroidUtilities.getSemiBoldTypeface(context));
        importTitle.setTextColor(Color.BLACK);

        TextView importSubTitle = new TextView(context);
        linearLayout.addView(importSubTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0,12));
        importSubTitle.setText("Enter your private key here to participate for the testnet bounty");
        importTitle.setTypeface(AndroidUtilities.getRegularTypeface(context));
        importSubTitle.setTextColor(Color.GRAY);

        GradientDrawable importKeyContainerBackground = new GradientDrawable();
        importKeyContainerBackground.setStroke(2, getResources().getColor(R.color.border));
        importKeyContainerBackground.setCornerRadius(12);

        LinearLayout importKeyContainer = new LinearLayout(context);
        linearLayout.addView(importKeyContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        importKeyContainer.setBackground(importKeyContainerBackground);
        importKeyContainer.setPadding(12, 12, 12, 12);


        EditText privateKeyEditText = new EditText(context);
        if (!Objects.equals(privateKey, defaultPrivateKey)) {
            privateKeyEditText.setText("*".repeat(privateKey.length()));
        }
        importKeyContainer.addView(privateKeyEditText);
        privateKeyEditText.setPadding(6, 6, 6, 6);
        privateKeyEditText.setHint("Enter your private key here.");
        privateKeyEditText.setBackground(null);
        privateKeyEditText.setTextSize(12);

        GradientDrawable saveButtonBackground = new GradientDrawable();
        saveButtonBackground.setCornerRadius(12);
        saveButtonBackground.setColor(getResources().getColor(R.color.primary));

        LinearLayout saveButton = new LinearLayout(context);
        linearLayout.addView(saveButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 12, 0, 0));
        saveButton.setBackground(saveButtonBackground);
        saveButton.setPadding(12, 12, 12, 12);
        saveButton.setOnClickListener(view -> {
            String pk = String.valueOf(privateKeyEditText.getText());
            // Decode the Base58-encoded private key
            byte[] privateKeyBytes = Base58.decode(pk);

            // Ensure the private key is 32 bytes
            if (privateKeyBytes.length != 64) {
                Toast.makeText(context, "Invalid private key entered", Toast.LENGTH_SHORT).show();
            } else {
                Keypair keyPair = Keypair.fromSecretKey(privateKeyBytes);
                String base58PublicKey = keyPair.getPublicKey().toBase58();
                prefs.setSolanaPrivateKey(pk);
                prefs.setSocksUsername(base58PublicKey);
                privateKeyEditText.clearFocus();
                finish();
                walletContainer.setVisibility(View.VISIBLE);
                Toast.makeText(context, "Wallet imported successfully🥳", Toast.LENGTH_SHORT).show();
            }
        });


        TextView saveButtonTitle = new TextView(context);
        saveButton.addView(saveButtonTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        saveButtonTitle.setGravity(Gravity.CENTER);
        saveButtonTitle.setText("Save");
        saveButtonTitle.setTextColor(Color.WHITE);
        setContentView(linearLayout);
        getBalance(wa);
    }

    private void getBalance(String solAddress) {
        Context context = WalletActivity.this;
        Dialog dialog = new Dialog(context);
        ProgressBar progressBar = new ProgressBar(context);

        walletController.getWalletOverview(solAddress, new DataCallbackInterface<PostWalletOverviewResponseDTO>() {
            @Override
            public void onSuccess(PostWalletOverviewResponseDTO overview) {
                balance.setText(String.valueOf(overview.balance));
                if (!overview.planExpired) {
                    upgradeButtonTitle.setText("Pro User⭐");
                    planExpired = false;
                }
                dialog.dismiss();
                if (!overview.clientConnected) {
                    getBootNodes();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                dialog.dismiss();
                Log.d("getNetworks", t.toString());
                Toast.makeText(context, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        dialog.setContentView(progressBar);
        dialog.show();
    }

    private void upgradeHandler() {
        Context context = WalletActivity.this;
        Dialog dialog = new Dialog(context);
        ProgressBar progressBar = new ProgressBar(context);

        dialog.setContentView(progressBar);
        dialog.show();

        if (!planExpired) {
            Toast.makeText(context, "You are a pro user already⭐", Toast.LENGTH_LONG).show();
            dialog.dismiss();
            return;
        }

        new Thread(() -> {
            try {
                CryptoService.sendTransaction(context);
                runOnUiThread(() -> {
                    Toast.makeText(context, "Verifying payment, check back later.", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                });
            }
        }).start();
    }

    private void getBootNodes() {
        Context context = WalletActivity.this;
        Dialog dialog = new Dialog(context);
        dialog.setCancelable(false);
        ProgressBar progressBar = new ProgressBar(context);

        networkController.getBootNodes(new DataCallbackInterface<List<Node>>() {
            @Override
            public void onSuccess(List<Node> nodes) {
                if(!nodes.isEmpty()) {
                    bootNodes = nodes;
                    Node node = bootNodes.get(0);
                    getNearestNode(node.ip, node.networkPort);
                    dialog.dismiss();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.d("getNetworks", t.toString());
                dialog.dismiss();
            }
        });

        dialog.setContentView(progressBar);
        dialog.show();
    }

    private void getNearestNode(String ip, int networkPort) {
        networkController.getNearestNode(ip, networkPort, new DataCallbackInterface<Node>() {
            @Override
            public void onSuccess(Node node) {
                // Connect to boot node and proxy
                connectToNearestNode(ip, networkPort, node.ip, node.networkPort);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.d("getNearestNode", t.toString());
            }
        });
    }

    private void connectToNearestNode(String nearestNodeIp, int nearestNodeNetworkPort, String targetNodeIp, int targetNodeNetworkPort) {
        Context context = WalletActivity.this;
        Dialog dialog = new Dialog(context);
        ProgressBar progressBar = new ProgressBar(context);

        String nodeApiUrl = Util.parseNodeAPIURL(nearestNodeIp, nearestNodeNetworkPort + 1);

        String sol_address = prefs.getSocksUsername();
        String pk = prefs.getSolanaPrivateKey();
        String signature = CryptoService.signMessage(pk);

        clientController.joinClient(nodeApiUrl, targetNodeIp, targetNodeNetworkPort, sol_address, signature, new DataCallbackInterface<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                dialog.dismiss();
            }

            @Override
            public void onFailure(Throwable t) {
                dialog.dismiss();
                Log.d("connectToNearestNode", t.toString());
                Toast.makeText(context, t.toString(), Toast.LENGTH_LONG).show();
            }
        });
        dialog.setContentView(progressBar);
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem tweetItem = menu.add(Menu.NONE, 1, Menu.NONE, "Tweet");

        // Set icon and display options
        tweetItem.setIcon(R.drawable.twitter);
        tweetItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            if (!planExpired) {
                handleTwitterPost();
                return false;
            } else {
                Toast.makeText(this, "Please upgrade to post on X.", Toast.LENGTH_SHORT).show();
                return true;
            }
        } else if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleTwitterPost() {
        String wa = prefs.getSocksUsername();
        String tweetText = "Just tried out this new decentralized VPN — full anonymity, zero logs, and no central servers 👀🔥\n" +
                "Surprisingly smooth and fast. Definitely one to watch.\n\n" +
                "Thanks for the heads-up @sheeldvpn @colosseum 💡\n" +
                "👉 https://sheeld.xyz\n\n" +
                "#Privacy\n" +
                wa.substring(wa.length() - 6);
        String tweetUrl = "https://twitter.com/intent/tweet?text=" + Uri.encode(tweetText);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(tweetUrl));
        startActivity(intent);
    }
}
