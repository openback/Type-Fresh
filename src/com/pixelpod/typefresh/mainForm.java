package com.pixelpod.typefresh;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ListView;
import android.widget.Toast;

public class mainForm extends ListActivity {
	public static final String TAG = "Type Fresh";
	// activity requestCode
	public static final int PICK_REQUEST_CODE = 0;
	// menu
	public static final int MENU_APPLY = 0;
	public static final int MENU_BACKUP = 1;
	public static final int MENU_RESTORE = 2;
	public static final int MENU_RESET = 3;
	// dialogs
	public static final int DIALOG_NEED_AND = 0;
	// handler dialog messages
	public static final int PDIAG_SET_TEXT = 0;
	public static final int PDIAG_DISMISS = 1;
	public static final int PDIAG_NEED_REBOOT = 2;
	public static final int DIAG_NOT_ROOTED = 3;
	// handler toast message
	public static final int TOAST_FONTS_APPLIED = 4;
	private String[] fonts;
	private String[] sysFontPaths;
	private String[] dstPaths = null;
	private String[] srcPaths = null;
	private String toastText;
	private int list_position;
	private final Runtime runtime = Runtime.getRuntime();
	private ProgressDialog pDiag = null;
	private FontListAdapter adapter = null;
	private static boolean backupExists = true;
	

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
	        	alert("Could not create Fonts directory on sdcard");
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
        	(new AlertDialog.Builder(this))
				.setIcon(android.R.drawable.ic_dialog_info)
				.setMessage(R.string.firstrun_message)
				.setTitle(R.string.firstrun_title)
				.setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();

							// Not firstrun anymore, so store that
							SharedPreferences settings = getPreferences(Activity.MODE_PRIVATE);
							SharedPreferences.Editor editor = settings.edit();
							editor.putBoolean("firstrun", false);
							editor.commit();
					}
				}).show();        	
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
		return true;
	}
	
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	menu.findItem(MENU_APPLY).setEnabled(!Arrays.equals(sysFontPaths, adapter.getPaths()));
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
			Log.e(TAG,"copyFonts: src and destination lenght mismatch. Quitting.");
			return;
		}
		
		srcPaths = src;
		dstPaths = dst;
		toastText = completedToast;
		
		pDiag = new ProgressDialog(this);
		pDiag.setTitle(dialogTitle);
		pDiag.setCancelable(false);
		pDiag.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		pDiag.show();

		new Thread() {
			public void run() {
				Looper.prepare();
				String cmd = null;
				boolean needReboot = false;
				Process su = null;
				
				try {
					remount("rw");

					for (int i = 0; i < srcPaths.length; i++) {
						if (srcPaths[i].equals(dstPaths[i])) {
							continue;
						}
						handler.sendMessage(Message.obtain(handler, PDIAG_SET_TEXT, srcPaths[i]));
						su = runtime.exec("/system/bin/su");
						cmd = "cp -f " + srcPaths[i] + " " + dstPaths[i];
						Log.i(TAG,"Executing \"" + cmd + "\"");
						cmd += "\nexit\n";

						su.getOutputStream().write(cmd.getBytes());

						if (su.waitFor() != 0) {
							BufferedReader br = new BufferedReader(new InputStreamReader(su.getErrorStream()), 200);
							String line;
							while((line = br.readLine()) != null) {
								Log.e(TAG,"Error copying: \"" + line + "\"");								
							}
							// even if there was an error, we want to continue to remount the system
						} else {
							// If we've overwritten any of the core fonts, we need to reboot
							if (dstPaths[i].indexOf("/system/fonts/Droid") == 0) {
								needReboot = true;
							}
						}
					}

					remount("ro");

					handler.sendEmptyMessage(TOAST_FONTS_APPLIED);

					if (needReboot) {
						handler.sendEmptyMessage(PDIAG_NEED_REBOOT);
					} else {
						handler.sendEmptyMessage(PDIAG_DISMISS);
					}
				} catch (IOException e) {
					Log.e(TAG,e.toString());
					handler.sendEmptyMessage(DIAG_NOT_ROOTED);
				} catch (InterruptedException e) {
					Log.e(TAG,e.toString());
				}
				handler.sendEmptyMessage(PDIAG_DISMISS);
			}
		}.start();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog dialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		switch (id) {
		case DIALOG_NEED_AND:
			builder = new AlertDialog.Builder(this);
			builder.setIcon(android.R.drawable.ic_dialog_alert)
				   .setMessage(R.string.need_and_message)
				   .setTitle("AndExplorer not found")
				   .setCancelable(false)
				   .setPositiveButton(R.string.need_and_ok, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   try {
				        	   Intent marketIntent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:lysesoft.andexplorer"));
	                           startActivity(marketIntent);
			        	   } catch (ActivityNotFoundException e) {
			        		   AlertDialog.Builder noMarketBuilder = new AlertDialog.Builder(mainForm.this);
			        		   noMarketBuilder.setIcon(android.R.drawable.ic_dialog_alert)
							   				  .setMessage(R.string.market_alert_message)
							                  .setTitle(R.string.no_market)
							                  .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
							                	  	public void onClick(DialogInterface dialog, int id) {
							                	  		dialog.cancel();
							                	  	}
							                  })
							                  .show();
			        	   }
			           }
			       })
			       .setNegativeButton(R.string.need_and_cancel, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                dialog.cancel();
			           }
			       });

			
			
			dialog = builder.create();
			break;
		default:
				dialog = null;
		}
		
		return dialog;
	}
	
	protected void alert(String msg) {
		AlertDialog diag = new AlertDialog.Builder(this).create();
		diag.setTitle(msg);
		diag.show();
	}
	
	protected void reboot() {
		pDiag = new ProgressDialog(this);
		pDiag.setTitle("Rebooting");
		pDiag.setMessage("Please wait.");
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

			(new AlertDialog.Builder(this))
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setMessage("Could not reboot the system. Please do so manually.")
				.setTitle("Reboot error")
				.setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
					}
				}
			).show();
		}
	}

	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
			case PDIAG_SET_TEXT:
				pDiag.setMessage((String)msg.obj);
				break;
			case PDIAG_DISMISS:
				pDiag.dismiss();
				break;
			case PDIAG_NEED_REBOOT:
				pDiag.dismiss();
				(new AlertDialog.Builder(mainForm.this))
							   .setIcon(android.R.drawable.ic_dialog_alert)
			   				   .setMessage(R.string.reboot_message)
			                   .setTitle(R.string.reboot_title)
			                   .setPositiveButton(R.string.reboot_ok, new DialogInterface.OnClickListener() {
			                	  	public void onClick(DialogInterface dialog, int id) {
			                	  		reboot();
			                	  		dialog.cancel();
			                	  	}
			                  })
			                   .setNegativeButton(R.string.reboot_cancel, new DialogInterface.OnClickListener() {
			                	  	public void onClick(DialogInterface dialog, int id) {
			                	  		dialog.cancel();
			                	  	}
			                  })
			                  .show();
				break;
			case DIAG_NOT_ROOTED:
				pDiag.dismiss();
				(new AlertDialog.Builder(mainForm.this))
							   .setIcon(android.R.drawable.ic_dialog_alert)
			   				   .setMessage("Error copying files. Are you rooted?")
			                   .setTitle("su command error")
			                   .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
			                	  	public void onClick(DialogInterface dialog, int id) {
			                	  		dialog.cancel();
			                	  	}
			                  })
			                  .show();
				break;
			case TOAST_FONTS_APPLIED:
				Toast.makeText(mainForm.this,toastText,Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};
	
	protected void remount(String type) {
		try {
			Process su = runtime.exec("/system/bin/su");
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
				return;
			} else {
				Log.i(TAG,"Remounted /system " + type);
			}
		} catch (Exception e) {
			
		}
		
	}
}