package net.pixelpod.typefresh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class FileCopier implements Runnable {
	private String[] dstPaths = null;
	private String[] srcPaths = null;
	private Handler handler   = null;

	public FileCopier(Handler msgHandler, String[] src, String[] dst) {
		handler = msgHandler;
		srcPaths = src;
		dstPaths = dst;
	}
	
	public void run() {
		Looper.prepare();
		String cmd = null;
		boolean needReboot = false;
		Process su = null;
		Runtime runtime = Runtime.getRuntime();
		
		if (!TypeFresh.remount("rw")) {
			handler.sendEmptyMessage(TypeFresh.DIALOG_REMOUNT_FAILED);
			return;
		}
		
		try {
			for (int i = 0; i < srcPaths.length; i++) {
				if (srcPaths[i].equals(dstPaths[i])) {
					continue;
				}
				handler.sendMessage(Message.obtain(handler, TypeFresh.PDIALOG_SET_TEXT, srcPaths[i]));
				su = runtime.exec("/system/bin/su");
				cmd = "cp -f " + srcPaths[i] + " " + dstPaths[i];
				Log.i(TypeFresh.TAG,"Executing \"" + cmd + "\"");
				cmd += "\nexit\n";

				su.getOutputStream().write(cmd.getBytes());

				if (su.waitFor() != 0) {
					BufferedReader br = new BufferedReader(new InputStreamReader(su.getErrorStream()), 200);
					String line;
					while((line = br.readLine()) != null) {
						Log.e(TypeFresh.TAG,"Error copying: \"" + line + "\"");								
					}
					// even if there was an error, we want to continue to remount the system
				} else {
					// If we've overwritten any of the core fonts, we need to reboot
					if (dstPaths[i].indexOf("/system/fonts/Droid") == 0) {
						needReboot = true;
					}
				}
			}

			if (!TypeFresh.remount("ro")) {
				handler.sendEmptyMessage(TypeFresh.DIALOG_REMOUNT_FAILED);
			}

			handler.sendEmptyMessage(TypeFresh.TOAST_FONTS_APPLIED);

			if (needReboot) {
				handler.sendEmptyMessage(TypeFresh.DIALOG_NEED_REBOOT);
			} else {
				handler.sendEmptyMessage(TypeFresh.PDIALOG_DISMISS);
			}
		} catch (IOException e) {
			Log.e(TypeFresh.TAG,e.toString());
			handler.sendEmptyMessage(TypeFresh.DIALOG_NOT_ROOTED);
		} catch (InterruptedException e) {
			Log.e(TypeFresh.TAG,e.toString());
		}
		handler.sendEmptyMessage(TypeFresh.PDIALOG_DISMISS);
	}
	
}