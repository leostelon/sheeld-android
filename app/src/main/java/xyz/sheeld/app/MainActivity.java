package xyz.sheeld.app;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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

public class MainActivity extends AppCompatActivity {
    // Animation Components
    private View connectButtonWaveView;
    private AnimatorSet animatorSet;
    private ObjectAnimator scaleX, scaleY, alpha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = new Preferences(this);

        EdgeToEdge.enable(this);
        Context  context = getApplicationContext();

        LinearLayout linearLayout = new LinearLayout(context);
        ViewCompat.setOnApplyWindowInsetsListener(linearLayout,(v, insets) -> {
            int topInset = insets.getSystemWindowInsetTop();
            linearLayout.setPadding(0, topInset, 0, 0);
            return insets;
        });

//        linearLayout.setBackgroundResource(R.drawable.home_background);
        linearLayout.setBackgroundColor(Color.WHITE);

        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(Gravity.TOP);

        TextView title = new TextView(context);
        linearLayout.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 32));
        title.setText("Sheeld");
        title.setTextColor(getResources().getColor(R.color.primary));
        title.setTypeface(AndroidUtilities.getMediumTypeface(context));
        title.setTextSize(32);
        title.setGravity(Gravity.CENTER);

        LinearLayout connectedStatusButton = new LinearLayout(context);
        linearLayout.addView(connectedStatusButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 0, 0, 12));
        connectedStatusButton.setPadding(24,12, 24, 12);

        GradientDrawable connectedStatusBackground = new GradientDrawable();
        connectedStatusBackground.setCornerRadius(24);
        connectedStatusBackground.setStroke(2, getResources().getColor(R.color.border));
        connectedStatusButton.setBackground(connectedStatusBackground);

        ImageView connectedStatusCheckIcon = new ImageView(context);
        connectedStatusButton.addView(connectedStatusCheckIcon, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 6, 0));
        connectedStatusCheckIcon.setImageResource(R.drawable.check);

        TextView startButtonTitle = new TextView(context);
        connectedStatusButton.addView(startButtonTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
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
        // Create the root layout
        FrameLayout connectButtonParentLayout = new FrameLayout(this);
        linearLayout.addView(connectButtonParentLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER, 0, 0, 0, 12));

        // Create the wave view (circle)
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
            // Start wave animation
            toggleWaveAnimation();
        });

        ImageView connectButtonIcon = new ImageView(context);
        connectButton.addView(connectButtonIcon, LayoutHelper.createLinear(300, 300, Gravity.CENTER));
        connectButtonIcon.setImageResource(R.drawable.power_button);
        connectButtonIcon.setForegroundGravity(Gravity.CENTER);

        // Add views to root layout
        connectButtonParentLayout.addView(connectButtonWaveView, LayoutHelper.createFrame(300, 300, Gravity.CENTER));   // behind
        connectButtonParentLayout.addView(connectButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER)); // on top

        setContentView(linearLayout);
    }

    private void toggleWaveAnimation() {
        if (animatorSet != null) {
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
}