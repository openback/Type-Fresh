package com.pixelpod.typefresh;
import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

public class FontListAdapter extends ArrayAdapter<Object> {
	private static final String TAG = "Type Fresh Adapter";
	LayoutInflater inflater;
	String[] fontNames = null;
	String[] fontPaths = null;

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

	public String[] getPaths() {
		return fontPaths;
	}
	
	public String[] getFonts() {
		return fontNames;
	}

	public void setFontPath(int position, String path) {
		fontPaths[position] = path;
		notifyDataSetChanged();
	}

	public void setFontPaths(String[] paths) {
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
	
	
	public static class ViewHolder {
		TextView font_name;
		TextView font_location;
		Button browse;
	}
}
