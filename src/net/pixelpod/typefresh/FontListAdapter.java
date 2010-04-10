/*
 * Copyright (C) 2010 Pixelpod INTERNATIONAL, Inc.
 * 
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package net.pixelpod.typefresh;

import net.pixelpod.typefresh.R;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
/**
 * Insert comments
 * 
 * @author Timothy Caraballo
 * @version 0.8
 */
public class FontListAdapter extends ArrayAdapter<Object> {
    // Tag to use for logging
    private static final String TAG = "Type Fresh Adapter";
    LayoutInflater inflater;
    String[] fontNames = null;
    String[] fontPaths = null;

    /**
     * Class constructor.
     * 
     * @param context The owner of this adapter.
     * @param fonts   A <code>String[]</code> containing all the system font filenames.
     */
    FontListAdapter(Activity context, String[] fonts) {
        super(context, R.layout.font_select, fonts);

        inflater = context.getLayoutInflater();
        fontNames = new String[fonts.length];
        fontPaths = new String[fonts.length];
        System.arraycopy(fonts, 0, this.fontNames, 0, fonts.length);

        for (int i = 0; i < fonts.length; i++) {
            fontPaths[i] = "/system/fonts/" + fonts[i];
        }
    }

    /**
     * Returns the user-selected font paths
     * 
     * @return <code>String[]</code> of paths in same order as in the <code>ListView</code>
     */
    public String[] getPaths() {
        return fontPaths;
    }
    
    /**
     * Returns the user-selected font path at the specified index
     * 
     * @param index index in list of fonts 
     * @return font path
     */
    public String getPathAt(int index) {
        return fontPaths[index];
    }
    
    /**
     * Returns the system font filenames
     * 
     * @return <code>String[]</code> of installed font filenames in same order as in the
     *          <code>ListView</code>
     */
    public String[] getFonts() {
        return fontNames;
    }

    /**
     * Sets a font at <code>position</code> to <code>path</code> to be applied later.
     * 
     * @param position Position in the <code>ListView</code> 
     * @param path     Full path of desired font.
     */
    public void setPathAt(int position, String path) {
        fontPaths[position] = path;
        notifyDataSetChanged();
    }

    /**
     * Sets all fonts desired paths at once to be applied later.
     * 
     * @param paths <code>String[]</code> of paths to be applied in same order as in the
     *               <code>ListView</code>
     */
    public void setPaths(String[] paths) {
        if (paths.length != fontPaths.length) {
            // TODO: throw exception?
            Log.i(TAG, "Not resetting paths");
        } else {
            System.arraycopy(paths, 0, fontPaths, 0, paths.length);
            notifyDataSetChanged();
        }
        
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;

        if (convertView == null) {    
            convertView = inflater.inflate(R.layout.font_select, null);
            
            holder = new ViewHolder();
            holder.font_name = (TextView) convertView.findViewById(R.id.font_name);
            holder.font_location = (TextView) convertView.findViewById(R.id.font_location);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        holder.font_name.setText(fontNames[position]);
        // don't display font path if it's the system font
        if (fontPaths[position].equals("/system/fonts/" + fontNames[position])) {
            holder.font_location.setVisibility(View.GONE);
        } else {
            holder.font_location.setText(fontPaths[position]);
            holder.font_location.setVisibility(View.VISIBLE);
        }
        return convertView;
    }
    
    /**
     * Class that holds frequently accessed references to reduce calls to <code>findViewById</code>.
     * 
     * @author Timothy Caraballo
     *
     */
    public static class ViewHolder {
        TextView font_name;
        TextView font_location;
        Button browse;
    }
}
