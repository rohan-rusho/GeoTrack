package com.rohan.geotrack.utils;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import androidx.core.content.ContextCompat;
import com.rohan.geotrack.R;

public class UIUtils {
    public static CharSequence getStyledAppName(Context context) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append("Geo");
        builder.setSpan(new ForegroundColorSpan(Color.WHITE), 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        int start = builder.length();
        builder.append("Track");
        builder.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.success)), start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        return builder;
    }

    public static CharSequence getStyledAppNameDark(Context context) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append("Geo");
        builder.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.text_primary)), 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        int start = builder.length();
        builder.append("Track");
        builder.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.success)), start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        return builder;
    }
}
