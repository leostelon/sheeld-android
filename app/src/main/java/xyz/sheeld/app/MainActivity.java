package xyz.sheeld.app;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

public class MainActivity extends AppCompatActivity implements DataUpdateListener {
    private TextView statusUploadSpeed;
    private TextView statusDownloadSpeed;
    private TextView startButtonTitle;
    private ImageView connectedStatusIcon;
    private static final int REQUEST_VPN_PERMISSION = 0x0F;
    private Preferences prefs;
    private static long[] oldStats = new long[]{0L, 0L, 0L, 0L};

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

        TextView title = new TextView(context);
        topNavigationContainer.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
        title.setText("Sheeld");
        title.setTextColor(getResources().getColor(R.color.primary));
        title.setTypeface(AndroidUtilities.getMediumTypeface(context));
        title.setTextSize(32);
        title.setGravity(Gravity.CENTER);

        LinearLayout settingsIconContainer = new LinearLayout(context);
        topNavigationContainer.addView(settingsIconContainer, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 12, 0, 0));
        settingsIconContainer.setPadding(4, 4, 4, 4);
        settingsIconContainer.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SelectCountryActivity.class);
            startActivity(intent);
        });
        GradientDrawable settingsGradient = new GradientDrawable();
        settingsGradient.setCornerRadius(12);
        settingsGradient.setStroke(2, getResources().getColor(R.color.border));
        settingsIconContainer.setBackground(settingsGradient);
        ImageView settingsIcon = new ImageView(context);
        settingsIconContainer.addView(settingsIcon, LayoutHelper.createLinear(50, 50));
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
        TextView timer = new TextView(context);
        linearLayout.addView(timer, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 0, 0, 16));
        timer.setText("00:04:12");
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
                startVPN(context);
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
        statusContainer.setOrientation(LinearLayout.HORIZONTAL);
        statusContainer.setPadding(0, 24, 0, 24);
        GradientDrawable statusContainerBackground = new GradientDrawable();
        statusContainer.setBackground(statusContainerBackground);
        statusContainerBackground.setStroke(2, getResources().getColor(R.color.border));
        statusContainerBackground.setCornerRadius(24);

        // Download
        LinearLayout statusDownloadContainer = new LinearLayout(context);
        statusContainer.addView(statusDownloadContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 1, 1));
        statusDownloadContainer.setOrientation(LinearLayout.HORIZONTAL);
        statusDownloadContainer.setGravity(Gravity.CENTER);

        ImageView statusDownloadIcon = new ImageView(context);
        statusDownloadContainer.addView(statusDownloadIcon, LayoutHelper.createLinear(48, 48, 0, 0, 12, 0));
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
        statusContainer.addView(divider, LayoutHelper.createLinear(4, 50,0, 8, 0, 8));

        // Upload
        LinearLayout statusUploadContainer = new LinearLayout(context);
        statusContainer.addView(statusUploadContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 1, 1));
        statusUploadContainer.setOrientation(LinearLayout.HORIZONTAL);
        statusUploadContainer.setGravity(Gravity.CENTER);

        ImageView statusUploadIcon = new ImageView(context);
        statusUploadContainer.addView(statusUploadIcon, LayoutHelper.createLinear(48, 48, 0, 0, 12, 0));
        statusUploadIcon.setImageResource(R.drawable.upload);

        LinearLayout statusUploadStats = new LinearLayout(context);
        statusUploadContainer.addView(statusUploadStats);
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
        updateStates();
    }

    private void toggleWaveAnimation(boolean isEnable) {
        if (isEnable) {
            stopWaveAnimation();
        } else {
            startWaveAnimation();
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

        prefs.setSocksAddress("192.168.18.4");
        prefs.setSocksPort(3000);

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
        toggleWaveAnimation(!isEnable);
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
        Log.d("MainActivitys", downloadSpeed);
        oldStats = stats;

        if (!isFinishing() && !isDestroyed()) {
        runOnUiThread(() -> {
            statusDownloadSpeed.setText(downloadSpeed);
            statusUploadSpeed.setText(uploadSpeed);
        });
        }
    }

    public static String formatBytes(long bytes) {
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
}