package xyz.sheeld.app;

import android.content.Context;
import android.graphics.Typeface;

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

    public static Typeface getLightTypeface(Context context) {
        return ResourcesCompat.getFont(context, R.font.poppins_light);
    }
    public static Typeface getRefularTypeface(Context context) {
        return ResourcesCompat.getFont(context, R.font.poppins_regular);
    }
    public static Typeface getMediumTypeface(Context context) {
        return ResourcesCompat.getFont(context, R.font.poppins_medium);
    }
    public static Typeface getSemiBoldTypeface(Context context) {
        return ResourcesCompat.getFont(context, R.font.poppins_semi_bold);
    }
}
