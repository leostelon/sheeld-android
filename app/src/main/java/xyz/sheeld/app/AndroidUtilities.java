package xyz.sheeld.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;

import java.util.Hashtable;

public class AndroidUtilities {
    private static final Hashtable<String, Typeface> typefaceCache = new Hashtable<>();

    public static float density = 1;
    public static int dp(float value) {
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }

    public static int dpToPx(int dp, Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    public static Typeface getLightTypeface(Context context) {
        return ResourcesCompat.getFont(context, R.font.poppins_light);
    }
    public static Typeface getRegularTypeface(Context context) {
        return ResourcesCompat.getFont(context, R.font.poppins_regular);
    }
    public static Typeface getMediumTypeface(Context context) {
        return ResourcesCompat.getFont(context, R.font.poppins_medium);
    }
    public static Typeface getSemiBoldTypeface(Context context) {
        return ResourcesCompat.getFont(context, R.font.poppins_semi_bold);
    }
    public static Typeface getBoldTypeface(Context context) {
        return ResourcesCompat.getFont(context, R.font.poppins_bold);
    }

    public static void copyToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
    }
}
