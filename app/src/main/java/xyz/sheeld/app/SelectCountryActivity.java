package xyz.sheeld.app;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.VpnService;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.ViewCompat;

import org.sol4k.Base58;
import org.sol4k.Keypair;

import java.util.List;
import java.util.Objects;

import xyz.sheeld.app.api.controllers.ClientController;
import xyz.sheeld.app.api.controllers.NetworkController;
import xyz.sheeld.app.api.interfaces.DataCallbackInterface;
import xyz.sheeld.app.api.types.Node;
import xyz.sheeld.app.vpn.VPN;

public class SelectCountryActivity extends AppCompatActivity {
    private LinearLayout nodesContainer;
    private final NetworkController networkController = new NetworkController();
    private final ClientController clientController = new ClientController();
    private Preferences prefs;
    private static final int REQUEST_VPN_PERMISSION = 0x0F;
    private static final String defaultPrivateKey = "53ESETwLEZbKFYtkC3G7qFGzFaTaTWXApU18EWXecVszdMbSDcbfyjxXYZ1fM45gEMMr9zJviN25GVFZV9htrshK";

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
            SpannableString s = new SpannableString("Settings");
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
        String privateKey = prefs.getSolanaPrivateKey();
        if (!Objects.equals(privateKey, defaultPrivateKey)) {
            privateKeyEditText.setText(privateKey);
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
                prefs.setSolanaPrivateKey(pk);
                prefs.setSocksUsername(keyPair.getPublicKey().toBase58());
                privateKeyEditText.clearFocus();
                finish();
                Toast.makeText(context, "Wallet imported successfully🥳", Toast.LENGTH_SHORT).show();
            }
        });


        TextView saveButtonTitle = new TextView(context);
        saveButton.addView(saveButtonTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        saveButtonTitle.setGravity(Gravity.CENTER);
        saveButtonTitle.setText("Save");
        saveButtonTitle.setTextColor(Color.WHITE);


        // Section Divider
        View sectionDivider = new View(context);
        sectionDivider.setBackgroundColor(getResources().getColor(R.color.border));
        linearLayout.addView(sectionDivider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1,0, 24, 0, 0));


        // Nodes List
        nodesContainer = new LinearLayout(context);
        linearLayout.addView(nodesContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 24, 0, 0));
        nodesContainer.setOrientation(LinearLayout.VERTICAL);

        setContentView(linearLayout);
        getNetworks();
    }

    private LinearLayout getNodeTile(Context context, Node node) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setStroke(2, getResources().getColor(R.color.border));
        gradientDrawable.setCornerRadius(12);

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setBackground(gradientDrawable);
        linearLayout.setPadding(12, 12, 24, 12);

        LinearLayout descriptionContainer = new LinearLayout(context);
        linearLayout.addView(descriptionContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 1, Gravity.CENTER));
        descriptionContainer.setOrientation(LinearLayout.VERTICAL);

        // Country Title
        TextView country = new TextView(context);
        descriptionContainer.addView(country);
        country.setText(node.location);
        country.setTextColor(Color.BLACK);
        country.setTypeface(AndroidUtilities.getMediumTypeface(context));

        // IP
        TextView ip = new TextView(context);
        descriptionContainer.addView(ip);
        ip.setText(node.ip+":"+node.networkPort);
        ip.setTextColor(Color.GRAY);
        ip.setTypeface(AndroidUtilities.getRegularTypeface(context));

        // Connect Icon
        TextView connect = new TextView(context);
        linearLayout.addView(connect, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT));
        connect.setText("Connect");
        connect.setGravity(Gravity.CENTER);
        connect.setTextColor(getResources().getColor(R.color.primary));
        connect.setTypeface(AndroidUtilities.getSemiBoldTypeface(context));
        connect.setOnClickListener(view -> {
            prefs.setNode(node);
            getNearestNode(node.ip, node.networkPort);
        });

        return  linearLayout;
    }

    private void getNearestNode(String ip, int networkPort) {
        Context context = SelectCountryActivity.this;
        Dialog dialog = new Dialog(context);
        ProgressBar progressBar = new ProgressBar(context);

       networkController.getNearestNode(ip, networkPort, new DataCallbackInterface<Node>() {
            @Override
            public void onSuccess(Node node) {
                dialog.dismiss();
                connectToNearestNode(node.ip, node.networkPort, ip, networkPort);
            }

            @Override
            public void onFailure(Throwable t) {
                dialog.dismiss();
                Log.d("getNetworks", t.toString());
            }
        });
        dialog.setContentView(progressBar);
        dialog.show();
    }

    private void connectToNearestNode(String nearestNodeIp, int nearestNodeNetworkPort, String targetNodeIp, int targetNodeNetworkPort) {
        Context context = SelectCountryActivity.this;
        Dialog dialog = new Dialog(context);
        ProgressBar progressBar = new ProgressBar(context);

        String nodeApiUrl = Util.parseNodeAPIURL(nearestNodeIp, nearestNodeNetworkPort+1);
        String sol_address = prefs.getSocksUsername();
        String pk = prefs.getSolanaPrivateKey();
        String signature = CryptoService.signMessage(pk);
        clientController.joinClient(nodeApiUrl, targetNodeIp, targetNodeNetworkPort, sol_address, signature, new DataCallbackInterface<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                dialog.dismiss();
                if (result) {
                    // Set preference to nearest network
                    VPN.getInstance(context).setNewNetwork(nearestNodeIp, nearestNodeNetworkPort);
                    // First stop current vpn
                    stopVPN();
                    // Then start the vpn
                    startVPN(context);
                    SocketClient.getInstance().connectToServer(Util.removeIpSchemes(nearestNodeIp), nearestNodeNetworkPort + 1);
                }
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

    private void getNetworks() {
        Context context = SelectCountryActivity.this;
        Dialog dialog = new Dialog(context);
        ProgressBar progressBar = new ProgressBar(context);

        networkController.getNodes(new DataCallbackInterface<List<Node>>() {
            @Override
            public void onSuccess(List<Node> nodes) {
                dialog.dismiss();
                nodes.forEach(node -> {
                    nodesContainer.addView(getNodeTile(context, node), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 6));
                });
            }

            @Override
            public void onFailure(Throwable t) {
                dialog.dismiss();
                Log.d("getNetworks", t.toString());
            }
        });
        dialog.setContentView(progressBar);
        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // VPN Function
    private void startVPN(Context context) {
        Intent intent = VpnService.prepare(context);
        if (intent != null) {
            startActivityForResult(intent, REQUEST_VPN_PERMISSION);
        } else {
            onActivityResult(REQUEST_VPN_PERMISSION, RESULT_OK, null);
        }

        prefs.setDnsIpv4("");
        prefs.setDnsIpv6("");

        Intent vpnIntent = new Intent(this, TProxyService.class);
        startService(vpnIntent.setAction(TProxyService.ACTION_CONNECT));
        Toast.makeText(context, "Connected to India", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void stopVPN() {
        Intent vpnIntent = new Intent(this, TProxyService.class);
        startService(vpnIntent.setAction(TProxyService.ACTION_DISCONNECT));
    }
}
