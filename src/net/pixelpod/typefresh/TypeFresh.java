package net.pixelpod.typefresh;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Arrays;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

public class TypeFresh extends ListActivity {
	public static final String TAG = "Type Fresh";
	// activity requestCode
	public static final int PICK_REQUEST_CODE = 0;
	// menu
	public static final int MENU_APPLY   = 0;
	public static final int MENU_BACKUP  = 1;
	public static final int MENU_RESTORE = 2;
	public static final int MENU_RESET   = 3;
	public static final int MENU_ABOUT   = 4;
	// Dialogs
	public static final int DIALOG_FIRSTRUN         =  1;
	public static final int DIALOG_ABOUT            =  2;
	public static final int DIALOG_NEED_ROOT        =  3;
	public static final int DIALOG_NEED_AND         =  4;
	public static final int DIALOG_NEED_REBOOT      =  5;
	public static final int DIALOG_NOT_ROOTED       =  6;
	public static final int DIALOG_COULD_NOT_REBOOT =  7;
	public static final int DIALOG_MKDIR_FAIL       =  8;
	public static final int DIALOG_NO_MARKET        =  9;
	public static final int DIALOG_REMOUNT_FAILED   = 10;
	// handler dialog messages
	public static final int PDIALOG_SET_TEXT = 0;
	public static final int PDIALOG_DISMISS  = 1;
	// handler toast message
	public static final int TOAST_FONTS_APPLIED = 4;
	private String toastText;
	private String[] fonts;
	private String[] sysFontPaths;
	private int list_position;
	private final Runtime runtime = Runtime.getRuntime();
	private ProgressDialog pDiag = null;
	private FontListAdapter adapter = null;
	private static boolean backupExists = true;
	
	// TODO: handle rotation while in the middle of work
	// TODO: Leaks from Dialogs when rotated

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        File fontsDir = new File("/system/fonts");
        fonts = fontsDir.list();
        Arrays.sort(fonts);
        sysFontPaths = new String[fonts.length];
        
    	File sdFonts = new File("/sdcard/Fonts");
    	if (!sdFonts.exists()) {
	        try {
	        	sdFonts.mkdir();
	        } catch (Exception e) {
	        	Log.e(TAG,e.toString());
	        	showDialog(DIALOG_MKDIR_FAIL);
	        }
    	}

    	for (int i = 0; i < fonts.length; i++) {
        	// check if any existing fonts are not backed up
        	if (!(new File("/sdcard/Fonts/" + fonts[i]).exists())) {
        		backupExists = false;
        	}
			sysFontPaths[i] = "/system/fonts/" + fonts[i];
		}
        
        setListAdapter(new FontListAdapter(this, fonts));
        registerForContextMenu(getListView());
        adapter = (FontListAdapter) this.getListAdapter();
        
        // restore paths on rotate
        if ((savedInstanceState != null) && savedInstanceState.containsKey("paths")) {
        	adapter.setFontPaths(savedInstanceState.getStringArray("paths"));
        }

        // do we need to show the welcome screen?
        SharedPreferences settings = getPreferences(Activity.MODE_PRIVATE);
        if (settings.getBoolean("firstrun", true)) {

        	// Not firstrun anymore, so store that
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("firstrun", false);
			editor.commit();

        	showDialog(DIALOG_FIRSTRUN);
		}
    }

	@Override
	public void onSaveInstanceState(Bundle bundle) {
		bundle.putStringArray("paths", adapter.fontPaths);
	}

    @Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
		list_position = position; 
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_PICK);
		Uri startDir = Uri.fromFile(new File("/sdcard/Fonts"));
		intent.setDataAndType(startDir, "vnd.android.cursor.dir/lysesoft.andexplorer.file");
		intent.putExtra("explorer_title", "Select a font");
		intent.putExtra("browser_title_background_color", "440000AA");
		intent.putExtra("browser_title_foreground_color", "FFFFFFFF");
		intent.putExtra("browser_list_background_color", "00000066");
		intent.putExtra("browser_list_fontscale", "120%");
		intent.putExtra("browser_list_layout", "0");
		intent.putExtra("browser_filter_extension_whitelist", "*.ttf");
		
		try {
			startActivityForResult(intent, PICK_REQUEST_CODE);
		} catch (ActivityNotFoundException e) {
			Log.e(TAG, e.toString());
			showDialog(DIALOG_NEED_AND);
		}
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == PICK_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				Uri uri = intent.getData();
				if (uri != null) {
					String path = uri.toString();
					if (path.toLowerCase().startsWith("file://")) {
						path = (new File(URI.create(path))).getAbsolutePath();
						adapter.setFontPath(list_position, path);
					}
				}
			}
		}
	}
	
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_APPLY, 0, "Apply fonts");
		menu.add(0, MENU_BACKUP, 0, "Backup fonts");
		menu.add(0, MENU_RESTORE, 0, "Restore fonts").setIcon(android.R.drawable.ic_menu_revert);
		menu.add(0, MENU_RESET, 0, "Reset paths").setIcon(R.drawable.ic_menu_clear_playlist);
		menu.add(0, MENU_ABOUT, 0, "About").setIcon(android.R.drawable.ic_menu_help);
		return true;
	}
	
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	boolean pathsSet = !Arrays.equals(sysFontPaths, adapter.getPaths());
    	menu.findItem(MENU_APPLY).setEnabled(pathsSet);
    	menu.findItem(MENU_RESET).setEnabled(pathsSet);
    	// TODO: Is there somewhere else I could do this?
    	backupExists = true;
		for (int i = 0; i < adapter.getFonts().length; i++) {
        	// check if any existing fonts are not backed up
        	if (!(new File("/sdcard/Fonts/" + fonts[i]).exists())) {
        		backupExists = false;
        		break;
        	}
		}
    	menu.findItem(MENU_RESTORE).setEnabled(backupExists);
    	return true;
    }
    
	/* Handles Menu item selections */
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case MENU_APPLY:
	    	applySelections();
	    	return true;
	    case MENU_BACKUP:
	    	backupFonts();
	        return true;
	    case MENU_RESTORE:
	    	restoreFonts();
	        return true;
	    case MENU_RESET:
	    	resetSelections();
	    	return true;
	    case MENU_ABOUT:
	       	showDialog(DIALOG_ABOUT);
	    	return true;
	    }
	    return false;
	}
	
	protected void backupFonts() {
		String[] dPaths = new String[fonts.length];
		for(int i = 0; i < fonts.length; i++) {
			dPaths[i] = "/sdcard/Fonts/" + fonts[i];
		}

		copyFiles("Backing up Fonts", "Fonts backed up to /sdcard/Fonts", sysFontPaths, dPaths);
	}
	
	protected void restoreFonts() {
		String[] sPaths = new String[fonts.length];
		for(int i = 0; i < sPaths.length; i++) {
			sPaths[i] = "/sdcard/Fonts/" + fonts[i];
		}
		copyFiles("Restoring Fonts", "Fonts restored from SD card", sPaths, sysFontPaths);

		resetSelections();
	}
	
	protected void resetSelections() {
		adapter.setFontPaths(sysFontPaths);		
	}	

	protected void applySelections() {
		String[] sPaths = adapter.getPaths();
		copyFiles("Applying Fonts", "Your fonts have been applied", sPaths, sysFontPaths);
		
	}	
	
	protected void copyFiles(String dialogTitle, String completedToast, String[] src, String[] dst) {
		if (src.length != dst.length) {
			Log.e(TAG,"copyFonts: src and destination lenght mismatch. Quitting copy.");
			return;
		}

		toastText = completedToast;

		pDiag = new ProgressDialog(this);
		pDiag.setTitle(dialogTitle);
		pDiag.setCancelable(false);
		pDiag.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		pDiag.show();

		(new Thread(new FileCopier(handler,src, dst))).start();
	}


	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog dialog;

		switch (id) {
		case DIALOG_FIRSTRUN:
			dialog = (new AlertDialog.Builder(this))
				.setIcon(android.R.drawable.ic_dialog_info)
				.setTitle(R.string.firstrun_title)
				.setMessage(R.string.firstrun_message)
				.setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
					}
				}
			).create();
			break;
		case DIALOG_ABOUT:
			dialog = (new AlertDialog.Builder(this))
				.setIcon(android.R.drawable.ic_dialog_info)
				.setTitle(R.string.about_title)
				.setMessage(R.string.about_message)
				.setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
					}
				}
			).create();
			break;
		case DIALOG_NEED_AND:
			dialog = (new AlertDialog.Builder(this))
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle("AndExplorer not found")
				.setMessage(R.string.need_and_message)
				.setPositiveButton(R.string.need_and_ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();

						try {
							Intent marketIntent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:lysesoft.andexplorer"));
					        startActivity(marketIntent);
						} catch (ActivityNotFoundException e) {
							showDialog(DIALOG_NO_MARKET);
						}
					}
				})
				.setNegativeButton(R.string.need_and_cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				}
			).create();
			break;
		case DIALOG_NEED_ROOT:
			dialog = (new AlertDialog.Builder(this))
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setMessage("Error copying files. Are you rooted?")
				.setTitle("su command error")
				.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				}
			).create();
			break;
		case DIALOG_NO_MARKET:
			dialog = (new AlertDialog.Builder(this))
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.no_market)
				.setMessage(R.string.market_alert_message)
				.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				}
			).create();
			break;
		case DIALOG_COULD_NOT_REBOOT:
			dialog = (new AlertDialog.Builder(this))
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle("Reboot error")
				.setMessage("Could not reboot the system. Please do so manually.")
				.setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
					}
				}
			).create();
			break;
		case DIALOG_MKDIR_FAIL:
			dialog = (new AlertDialog.Builder(this))
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle("Error")
				.setMessage("Could not create Fonts directory on sdcard")
				.setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
					}
				}
			).create();
			break;
		case DIALOG_REMOUNT_FAILED:
			dialog = (new AlertDialog.Builder(this))
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle("Error")
				.setMessage("Could not remount /system/fonts")
				.setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
					}
				}
			).create();
			break;
		case DIALOG_NEED_REBOOT:
			dialog = (new AlertDialog.Builder(this))
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setMessage(R.string.reboot_message)
				.setTitle(R.string.reboot_title)
				.setPositiveButton(R.string.reboot_ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
						reboot();
					}
				})
				.setNegativeButton(R.string.reboot_cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				}
			).create();
			break;
		default:
			dialog = null;
		}
		
		return dialog;
	}

	protected void reboot() {
		pDiag = new ProgressDialog(this);
		pDiag.setTitle("Rebooting");
		pDiag.setMessage("Please wait...");
		pDiag.show();

		try {
			Log.i(TAG,"Calling reboot");
			Process su = runtime.exec("/system/bin/su");
			su.getOutputStream().write("reboot".getBytes());
/*			if (su.waitFor() != 0) {
				BufferedReader br = new BufferedReader(new InputStreamReader(su.getErrorStream()), 200);
				String line;
				while((line = br.readLine()) != null) {
					Log.e(TAG,"Error copying: \"" + line + "\"");								
				}
				// even if there was an error, we want to continue to remount the system
			}
*/
		} catch (Exception e) {
			Log.e(TAG, e.toString());
			pDiag.dismiss();
			showDialog(DIALOG_COULD_NOT_REBOOT);
		}
	}

	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
			case PDIALOG_SET_TEXT:
				pDiag.setMessage((String)msg.obj);
				break;
			case PDIALOG_DISMISS:
				pDiag.dismiss();
				break;
			case DIALOG_NEED_REBOOT:
				pDiag.dismiss();
				showDialog(DIALOG_NEED_REBOOT);
				break;
			case DIALOG_NOT_ROOTED:
				pDiag.dismiss();
				showDialog(DIALOG_NOT_ROOTED);
				break;
			case TOAST_FONTS_APPLIED:
				Toast.makeText(TypeFresh.this,toastText,Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};
	
	public static boolean remount(String type) {
		try {
			Process su = Runtime.getRuntime().exec("/system/bin/su");
			Log.i(TAG,"Remounting /system " + type);
			String cmd = "mount -o remount," + type + " -t yaffs2 /dev/block/mtdblock3 /system\nexit\n";
			su.getOutputStream().write(cmd.getBytes());
			
			if (su.waitFor() != 0) {
				BufferedReader br = new BufferedReader(new InputStreamReader(su.getErrorStream()), 200);
				String line;
				while((line = br.readLine()) != null) {
					Log.e(TAG,"Error remounting: \"" + line + "\"");
				}
				Log.e(TAG, "Could not remount, returning");
				return false;
			} else {
				Log.i(TAG,"Remounted /system " + type);
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}
}