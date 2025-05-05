package xyz.sheeld.app;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.VpnService;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.ViewCompat;

import java.util.List;

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

        // Nodes List
        nodesContainer = new LinearLayout(context);
        linearLayout.addView(nodesContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
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
        country.setText("India");
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
                Toast.makeText(context, "fetched", Toast.LENGTH_SHORT).show();
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
        clientController.joinClient(nodeApiUrl, targetNodeIp, targetNodeNetworkPort, sol_address, new DataCallbackInterface<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                dialog.dismiss();
                if (result) {
                    // connect to vpn here
                    // Set preference to nearest network
                    VPN.getInstance(context).setNewNetwork(nearestNodeIp, nearestNodeNetworkPort);
                    // First stop current vpn
                    stopVPN();
                    // Then start the vpn
                    startVPN(context);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                dialog.dismiss();
                Log.d("connectToNearestNode", t.toString());
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
