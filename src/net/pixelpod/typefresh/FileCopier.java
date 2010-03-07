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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

/**
 * Insert comments.
 * 
 * @author Timothy Caraballo
 *
 */
public class FileCopier extends AsyncTask<Object, Object, Void> {
    private String mToastText = "";
    private String[] dstPaths = null;
    private String[] srcPaths = null;
    private TypeFresh mTypeFresh = null;

    /**
     * Class constructor.
     * 
     * @param typeFresh The owner of this copier and owner of the
     *                   ProgressDialog that we will use.
     */
    public FileCopier(TypeFresh typeFresh) {
        mTypeFresh = typeFresh;
    }

    @Override

    // params: String[] source, String[] destination, toastText 
    protected Void doInBackground(Object... params) {
        srcPaths = (String[])params[0];
        dstPaths = (String[])params[1];
        mToastText = (String)params[2];

        Looper.prepare();
        String cmd = null;
        boolean needReboot = false;
        Process su = null;
        Runtime runtime = Runtime.getRuntime();

        try {
            if (!TypeFresh.remount(TypeFresh.READ_WRITE)) {
                publishProgress(TypeFresh.DIALOG_REMOUNT_FAILED);
                return null;
            }
        } catch (IOException e) {
            Log.e(TypeFresh.TAG, e.toString());
            publishProgress(TypeFresh.DIALOG_REMOUNT_FAILED);
            return null;
        } catch (InterruptedException e) {
            Log.e(TypeFresh.TAG, e.toString());
            publishProgress(TypeFresh.DIALOG_REMOUNT_FAILED);
            return null;
        }
        
        try {
            for (int i = 0; i < srcPaths.length; i++) {
                if (srcPaths[i].equals(dstPaths[i])) {
                    continue;
                }
                publishProgress(srcPaths[i]);
                su = runtime.exec("/system/bin/su");
                cmd = "cp -f " + srcPaths[i] + " " + dstPaths[i];
                Log.i(TypeFresh.TAG,"Executing \"" + cmd + "\"");
                cmd += "\nexit\n";

                su.getOutputStream().write(cmd.getBytes());

                if (su.waitFor() != 0) {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(su.getErrorStream()), 200);
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
                // clear up references, since we're done
                su.destroy();
            }

            if (!TypeFresh.remount(TypeFresh.READ_ONLY)) {
                publishProgress(TypeFresh.DIALOG_REMOUNT_FAILED);
            }

            if (needReboot) {
                publishProgress(TypeFresh.DIALOG_NEED_REBOOT);
            }
        } catch (IOException e) {
            Log.e(TypeFresh.TAG,e.toString());
            publishProgress(TypeFresh.DIALOG_NEED_ROOT);
        } catch (InterruptedException e) {
            Log.e(TypeFresh.TAG,e.toString());
            // hmm...should I yell about this to the user?
        }
        return null;
    }


    @Override
    protected void onProgressUpdate(Object... message) {
        if (message[0] instanceof String) {
            // a String will just update the ProgressDialog
            mTypeFresh.mPDialog.setMessage((String)message[0]);
        } else {
            // otherwise we're calling another Dialog
            mTypeFresh.showDialog(((Number)message[0]).intValue());
        }
    }
    
    @Override
    protected void onPreExecute() {
        mTypeFresh.showDialog(TypeFresh.DIALOG_PROGRESS);
    }
    
    @Override
    protected void onPostExecute(Void result) {
        mTypeFresh.mPDialog.dismiss();
        Toast.makeText(mTypeFresh, mToastText, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Sets a new TypeFresh as this threads owner. This is important, since we
     * need a reference to the new Activity when the screen rotates.
     * 
     * @param typeFresh The new Activity whose ProgressDialog to use.
     */
    public void setActivity(TypeFresh typeFresh) {
        mTypeFresh = typeFresh;
    }

}
