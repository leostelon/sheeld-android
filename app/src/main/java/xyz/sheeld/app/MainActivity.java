package xyz.sheeld.app;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.VpnService;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import java.util.ArrayList;
import java.util.List;

import xyz.sheeld.app.api.controllers.ClientController;
import xyz.sheeld.app.api.controllers.NetworkController;
import xyz.sheeld.app.api.interfaces.DataCallbackInterface;
import xyz.sheeld.app.api.types.Node;
import xyz.sheeld.app.vpn.VPN;

public class MainActivity extends AppCompatActivity implements DataUpdateListener {
    private TextView statusUploadSpeed;
    private TextView statusDownloadSpeed;
    private TextView startButtonTitle;
    private ImageView connectedStatusIcon;
    private TextView timer;
    private static final int REQUEST_VPN_PERMISSION = 0x0F;
    private Preferences prefs;
    private static long[] oldStats = new long[]{0L, 0L, 0L, 0L};
    private final NetworkController networkController = new NetworkController();
    private final ClientController clientController = new ClientController();
    private Node node;
    private TextView statusCountryIp;
    private TextView statusDownloadCountry;
    private SpannableString statusDownloadCountrySpannable;
    private TextView latency;
    private List<Node> circuit;

    // Animation Components
    private View connectButtonWaveView;
    private AnimatorSet animatorSet;
    private ObjectAnimator scaleX, scaleY, alpha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DataManager.getInstance().isListenerActive()) {
            DataManager.getInstance().addListener(this);
        }

        prefs = new Preferences(this);

        EdgeToEdge.enable(this);
        Context  context = getApplicationContext();

        LinearLayout linearLayout = new LinearLayout(context);
        ViewCompat.setOnApplyWindowInsetsListener(linearLayout,(v, insets) -> {
            int topInset = insets.getSystemWindowInsetTop();
            int bottomInset = insets.getSystemWindowInsetBottom();
            linearLayout.setPadding(32, topInset, 32, bottomInset);
            return insets;
        });

//        linearLayout.setBackgroundResource(R.drawable.home_background);
        linearLayout.setBackgroundColor(Color.WHITE);

        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(Gravity.TOP);

        FrameLayout topNavigationContainer = new FrameLayout(context);
        linearLayout.addView(topNavigationContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 32));

        LinearLayout titleContainer = new LinearLayout(context);
        topNavigationContainer.addView(titleContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
        titleContainer.setOrientation(LinearLayout.HORIZONTAL);
        titleContainer.setGravity(Gravity.CENTER);

        ImageView sheeldIcon = new ImageView(context);
        titleContainer.addView(sheeldIcon, LayoutHelper.createLinear(200, 150));
        sheeldIcon.setImageResource(R.drawable.logo);

        // Wallet Settings
        LinearLayout walletIconParentContainer = new LinearLayout(context);
        topNavigationContainer.addView(walletIconParentContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 30, 0, 0));
        walletIconParentContainer.setGravity(Gravity.START);

        LinearLayout walletIconContainer = new LinearLayout(context);
        walletIconParentContainer.addView(walletIconContainer, LayoutHelper.createLinear(60, 60, 0, 12, 0, 0));
        walletIconContainer.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, WalletActivity.class);
            startActivity(intent);
        });
        walletIconContainer.setGravity(Gravity.CENTER);
        GradientDrawable walletGradient = new GradientDrawable();
        walletGradient.setCornerRadius(12);
        walletGradient.setStroke(2, getResources().getColor(R.color.border));
        walletIconContainer.setBackground(walletGradient);
        ImageView walletIcon = new ImageView(context);
        walletIconContainer.addView(walletIcon, LayoutHelper.createLinear(30, 30));
        walletIcon.setImageResource(R.drawable.adjustments);

        // Settings
        LinearLayout settingsIconParentContainer = new LinearLayout(context);
        topNavigationContainer.addView(settingsIconParentContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 30, 0, 0));
        settingsIconParentContainer.setGravity(Gravity.END);

        LinearLayout settingsIconContainer = new LinearLayout(context);
        settingsIconParentContainer.addView(settingsIconContainer, LayoutHelper.createLinear(60, 60, 0, 12, 0, 0));
        settingsIconContainer.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SelectCountryActivity.class);
            startActivity(intent);
        });
        settingsIconContainer.setGravity(Gravity.CENTER);
        GradientDrawable settingsGradient = new GradientDrawable();
        settingsGradient.setCornerRadius(12);
        settingsGradient.setStroke(2, getResources().getColor(R.color.border));
        settingsIconContainer.setBackground(settingsGradient);
        ImageView settingsIcon = new ImageView(context);
        settingsIconContainer.addView(settingsIcon, LayoutHelper.createLinear(30, 30));
        settingsIcon.setImageResource(R.drawable.adjustments);

        LinearLayout connectedStatusButton = new LinearLayout(context);
        linearLayout.addView(connectedStatusButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 0, 0, 12));
        connectedStatusButton.setPadding(24,12, 24, 12);

        GradientDrawable connectedStatusBackground = new GradientDrawable();
        connectedStatusBackground.setCornerRadius(24);
        connectedStatusBackground.setStroke(2, getResources().getColor(R.color.border));
        connectedStatusButton.setBackground(connectedStatusBackground);

        connectedStatusIcon = new ImageView(context);
        connectedStatusButton.addView(connectedStatusIcon, LayoutHelper.createLinear(40, 40, 0, 0, 6, 0));
        connectedStatusIcon.setImageResource(R.drawable.connected_icon);

        startButtonTitle = new TextView(context);
        connectedStatusButton.addView(startButtonTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT));
        startButtonTitle.setGravity(Gravity.CENTER);
        startButtonTitle.setText("Connected");
        startButtonTitle.setTextColor(Color.BLACK);

        // Timer
        timer = new TextView(context);
        linearLayout.addView(timer, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 0, 0, 16));
        timer.setText("00:00:00");
        timer.setTypeface(AndroidUtilities.getSemiBoldTypeface(context));
        timer.setTextColor(Color.BLACK);
        timer.setTextSize(40);

        // Connect Button
        FrameLayout connectButtonParentLayout = new FrameLayout(this);
        linearLayout.addView(connectButtonParentLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 1, Gravity.CENTER));

        connectButtonWaveView = new View(this);
        GradientDrawable circleDrawable = new GradientDrawable();
        circleDrawable.setShape(GradientDrawable.OVAL);
        circleDrawable.setColor(0x884488FF); // semi-transparent light blue
        connectButtonWaveView.setBackground(circleDrawable);
        connectButtonWaveView.setScaleX(0f);
        connectButtonWaveView.setScaleY(0f);
        connectButtonWaveView.setAlpha(0f);

        LinearLayout connectButton = new LinearLayout(context);
        connectButton.setPadding(24,12, 24, 12);
        connectButton.setOnClickListener(view -> {
            boolean isEnable = prefs.getEnable();
            prefs.setEnable(!isEnable);
            if (isEnable) {
                stopVPN();
            } else {
                if (node != null) {
                    getNearestNode(node.ip, node.networkPort);
                }
            }
        });

        ImageView connectButtonIcon = new ImageView(context);
        connectButton.addView(connectButtonIcon, LayoutHelper.createLinear(300, 300, Gravity.CENTER));
        connectButtonIcon.setImageResource(R.drawable.power_button);
        connectButtonIcon.setForegroundGravity(Gravity.CENTER);

        connectButtonParentLayout.addView(connectButtonWaveView, LayoutHelper.createFrame(300, 300, Gravity.CENTER));   // behind
        connectButtonParentLayout.addView(connectButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER)); // on top

        ImageView globeImage = new ImageView(context);
        linearLayout.addView(globeImage, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, -24));
        globeImage.setImageResource(R.drawable.globe);

        // Status Containers
        LinearLayout statusContainer = new LinearLayout(context);
        linearLayout.addView(statusContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        statusContainer.setOrientation(LinearLayout.VERTICAL);
        statusContainer.setPadding(48, 24, 48, 24);
        GradientDrawable statusContainerBackground = new GradientDrawable();
        statusContainer.setBackground(statusContainerBackground);
        statusContainerBackground.setStroke(2, getResources().getColor(R.color.border));
        statusContainerBackground.setCornerRadius(24);

        // Country Details Container
        LinearLayout statusCountryStatsParent = new LinearLayout(context);
        statusContainer.addView(statusCountryStatsParent, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        LinearLayout statusCountryStats = new LinearLayout(context);
        statusCountryStatsParent.addView(statusCountryStats, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER));
        statusCountryStats.setOrientation(LinearLayout.VERTICAL);

        statusDownloadCountry = new TextView(context);
        statusCountryStats.addView(statusDownloadCountry);
        statusDownloadCountry.setTypeface(AndroidUtilities.getMediumTypeface(context));
        statusDownloadCountry.setOnClickListener(view -> {
            if (circuit != null && circuit.size() == 2) {
                String message = "Connected to " + circuit.get(1).location + " via " + circuit.get(0).location;
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });

        statusCountryIp = new TextView(context);
        statusCountryStats.addView(statusCountryIp);
        statusCountryIp.setTypeface(AndroidUtilities.getRegularTypeface(context));

        // Latency
        latency = new TextView(context);
        statusCountryStatsParent.addView(latency, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT));
        latency.setGravity(Gravity.CENTER);
        latency.setText("0ms");
        latency.setTextColor(Color.GRAY);

        // Section Divider
        View sectionDivider = new View(context);
        sectionDivider.setBackgroundColor(getResources().getColor(R.color.border));
        statusContainer.addView(sectionDivider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1,0, 8, 0, 16));

        // Download
        LinearLayout statusBandwidthContainer = new LinearLayout(context);
        statusContainer.addView(statusBandwidthContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        statusBandwidthContainer.setOrientation(LinearLayout.HORIZONTAL);
        statusBandwidthContainer.setGravity(Gravity.CENTER);

        LinearLayout statusDownloadContainer = new LinearLayout(context);
        statusBandwidthContainer.addView(statusDownloadContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 1, 1));
        statusDownloadContainer.setOrientation(LinearLayout.HORIZONTAL);
        statusDownloadContainer.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

        ImageView statusDownloadIcon = new ImageView(context);
        statusDownloadContainer.addView(statusDownloadIcon, LayoutHelper.createLinear(48, 48, 0, 0, 24, 0));
        statusDownloadIcon.setImageResource(R.drawable.download);

        LinearLayout statusDownloadStats = new LinearLayout(context);
        statusDownloadContainer.addView(statusDownloadStats);
        statusDownloadStats.setOrientation(LinearLayout.VERTICAL);

        statusDownloadSpeed = new TextView(context);
        statusDownloadStats.addView(statusDownloadSpeed);
        statusDownloadSpeed.setTypeface(AndroidUtilities.getMediumTypeface(context));
        statusDownloadSpeed.setText("0 MB/s");

        TextView statusDownloadSpeedTitle = new TextView(context);
        statusDownloadStats.addView(statusDownloadSpeedTitle);
        statusDownloadSpeedTitle.setText("Download");
        statusDownloadSpeedTitle.setTypeface(AndroidUtilities.getRegularTypeface(context));

        // Divider
        View divider = new View(context);
        divider.setBackgroundColor(getResources().getColor(R.color.border));
        statusBandwidthContainer.addView(divider, LayoutHelper.createLinear(4, 50,0, 8, 0, 8));

        // Upload
        LinearLayout statusUploadContainer = new LinearLayout(context);
        statusBandwidthContainer.addView(statusUploadContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 1, 1));
        statusUploadContainer.setOrientation(LinearLayout.HORIZONTAL);
        statusUploadContainer.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

        ImageView statusUploadIcon = new ImageView(context);
        statusUploadContainer.addView(statusUploadIcon, LayoutHelper.createLinear(48, 48));
        statusUploadIcon.setImageResource(R.drawable.upload);

        LinearLayout statusUploadStats = new LinearLayout(context);
        statusUploadContainer.addView(statusUploadStats, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 24, 0, 0, 0));
        statusUploadStats.setOrientation(LinearLayout.VERTICAL);

        statusUploadSpeed = new TextView(context);
        statusUploadStats.addView(statusUploadSpeed);
        statusUploadSpeed.setText("0 MB/s");
        statusUploadSpeed.setTypeface(AndroidUtilities.getMediumTypeface(context));

        TextView statusUploadSpeedTitle = new TextView(context);
        statusUploadStats.addView(statusUploadSpeedTitle);
        statusUploadSpeedTitle.setText("Upload");
        statusUploadSpeedTitle.setTypeface(AndroidUtilities.getRegularTypeface(context));

        setContentView(linearLayout);
        getCurrentNode();
        updateStates();
    }

    private void toggleWaveAnimation(boolean isEnable) {
        if (isEnable) {
            startWaveAnimation();
        } else {
            stopWaveAnimation();
        }
    }

    private void startWaveAnimation() {
        // Scale and fade animation
        scaleX = ObjectAnimator.ofFloat(connectButtonWaveView, View.SCALE_X, 0f, 2f);
        scaleY = ObjectAnimator.ofFloat(connectButtonWaveView, View.SCALE_Y, 0f, 2f);
        alpha = ObjectAnimator.ofFloat(connectButtonWaveView, View.ALPHA, 1f, 0f);

        // Set repeat count & mode on each animator
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setRepeatMode(ValueAnimator.RESTART);

        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatMode(ValueAnimator.RESTART);

        alpha.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setRepeatMode(ValueAnimator.RESTART);

        animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, alpha);
        animatorSet.setDuration(1200);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }

    private void stopWaveAnimation() {
        if (animatorSet != null) {
            animatorSet.cancel();
            animatorSet = null;
        }

        // Reset wave view
        connectButtonWaveView.setAlpha(0f);
        connectButtonWaveView.setScaleX(0f);
        connectButtonWaveView.setScaleY(0f);
    }

    private void startVPN(Context context) {
        Intent intent = VpnService.prepare(context);
        if (intent != null) {
            startActivityForResult(intent, REQUEST_VPN_PERMISSION);
        } else {
            onActivityResult(REQUEST_VPN_PERMISSION, RESULT_OK, null);
        }

        Intent vpnIntent = new Intent(this, TProxyService.class);

        prefs.setDnsIpv4("");
        prefs.setDnsIpv6("");

        startService(vpnIntent.setAction(TProxyService.ACTION_CONNECT));
        updateStates();
    }

    private void stopVPN() {
        Intent vpnIntent = new Intent(this, TProxyService.class);
        startService(vpnIntent.setAction(TProxyService.ACTION_DISCONNECT));
        updateStates();
    }

    private void updateStates() {
        boolean isEnable = prefs.getEnable();
        toggleWaveAnimation(isEnable);
        String connectedStatusTitle = isEnable ? "Connected" : "Not Connected";
        startButtonTitle.setText(connectedStatusTitle);
        int connectStatusIcon = isEnable ? R.drawable.connected_icon : R.drawable.not_connected_icon;
        connectedStatusIcon.setImageResource(connectStatusIcon);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        DataManager.getInstance().addListener(this);
        updateStates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        DataManager.getInstance().removeListener(this);
    }

    @Override
    public void onDataUpdated(long[] stats) {
        String downloadSpeed = formatBytes(stats[3] - oldStats[3]);
        String uploadSpeed = formatBytes(stats[1] - oldStats[1]);
        oldStats = stats;

        if (!isFinishing() && !isDestroyed()) {
        runOnUiThread(() -> {
            statusDownloadSpeed.setText(downloadSpeed);
            statusUploadSpeed.setText(uploadSpeed);
        });
        }
    }

    @Override
    public void onTimeUpdated(long time) {
        if (!isFinishing() && !isDestroyed()) {
            runOnUiThread(() -> {
                timer.setText(convertSecondsToTimeString(time));
            });
        }
    }

    @Override
    public void onLatencyUpdated(int l) {
        if (!isFinishing() && !isDestroyed()) {
            runOnUiThread(() -> {
                String formattedLatency = l + "ms";
                latency.setText(formattedLatency);
            });
        }
    }

    @Override
    public void onCircuitUpdated(List<Node> c) {
        circuit = c;
        node = c.get(1);
        String path = c.get(1).location + "(" + c.get(0).location+")";
        statusDownloadCountrySpannable = new SpannableString(path);
        statusDownloadCountrySpannable.setSpan(new ForegroundColorSpan(Color.GRAY), c.get(1).location.length(), path.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        statusDownloadCountrySpannable.setSpan(new RelativeSizeSpan(0.5f), c.get(1).location.length(), path.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        statusDownloadCountry.setText(statusDownloadCountrySpannable);
    }

    public static String formatBytes(long bytes) {
        if (bytes < 0) {
            bytes = 0;
        }
        long kb = 1024;
        long mb = kb * 1024;
        if (bytes < kb) {
            return bytes + " bytes";
        } else if (bytes < mb) {
            double resultKb = (double) bytes / kb;
            return String.format("%.2f KB/s", resultKb);
        } else {
            double resultMb = (double) bytes / mb;
            return String.format("%.2f MB/s", resultMb);
        }
    }

    private void getCurrentNode() {
        node = prefs.getNode();
        if (node == null) {
            getNodes();
        } else {
            updateCurrentNodeUI(node);
        }
    }

    private void getNodes() {
        Context context = MainActivity.this;
        Dialog dialog = new Dialog(context);
        ProgressBar progressBar = new ProgressBar(context);

        networkController.getNodes(new DataCallbackInterface<List<Node>>() {
            @Override
            public void onSuccess(List<Node> nodes) {
                dialog.dismiss();
                if(!nodes.isEmpty()) {
                    node = nodes.get(0);
                    prefs.setNode(node);
                    updateCurrentNodeUI(node);
                }
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

    private void updateCurrentNodeUI(Node node) {
        statusCountryIp.setText(Util.removeIpSchemes(node.ip));
        statusDownloadCountry.setText(node.location);
    }

    private void getNearestNode(String ip, int networkPort) {
        Context context = MainActivity.this;
        Dialog dialog = new Dialog(context);
        ProgressBar progressBar = new ProgressBar(context);

        networkController.getNearestNode(ip, networkPort, new DataCallbackInterface<Node>() {
            @Override
            public void onSuccess(Node nearestNode) {
                dialog.dismiss();
                List<Node> circuit = new ArrayList<>();
                circuit.add(nearestNode);
                circuit.add(node);
                DataManager.getInstance().setCircuit(circuit);
                connectToNearestNode(nearestNode.ip, nearestNode.networkPort, ip, networkPort);
            }

            @Override
            public void onFailure(Throwable t) {
                dialog.dismiss();
                Log.d("getNearestNode", t.toString());
            }
        });
        dialog.setContentView(progressBar);
        dialog.show();
    }

    private void connectToNearestNode(String nearestNodeIp, int nearestNodeNetworkPort, String targetNodeIp, int targetNodeNetworkPort) {
        Context context = MainActivity.this;
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
                if (result) {
                    // connect to vpn here
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

    private static String convertSecondsToTimeString(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}