package medev.gabreek.flyffucommander;

import android.content.Context;
import android.util.DisplayMetrics;

public class DisplayUtils {

    private final Context context;

    public DisplayUtils(Context context) {
        this.context = context;
    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}
