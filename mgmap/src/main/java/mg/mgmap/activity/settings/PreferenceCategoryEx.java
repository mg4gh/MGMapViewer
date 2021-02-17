/*
 * Copyright 2017 - 2021 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.activity.settings;

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