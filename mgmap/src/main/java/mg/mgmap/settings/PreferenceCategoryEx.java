package mg.mgmap.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceViewHolder;

public class PreferenceCategoryEx extends PreferenceCategory
{

    public PreferenceCategoryEx(Context ctx, AttributeSet attrs, int defStyle)
    {
        super(ctx, attrs, defStyle);
    }

    public PreferenceCategoryEx(Context ctx, AttributeSet attrs)
    {
        super(ctx, attrs);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder)
    {
        super.onBindViewHolder(holder);
        TextView summary= (TextView)holder.findViewById(android.R.id.summary);
        if (summary != null)
        {
            // Enable multiple line support
            summary.setSingleLine(false);
            summary.setMaxLines(10); // Just need to be high enough I guess
        }
    }

}