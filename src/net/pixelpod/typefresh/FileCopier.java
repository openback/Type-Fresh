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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

/**
 * @author Timothy Caraballo
 *
 */
public class FileCopier extends AsyncTask<Object, Object, Void> {
    // for remount()
    public static final int READ_ONLY  = 0;
    public static final int READ_WRITE = 1;

    // holds block that /system is located on
    private static String systemBlock = null;
    private String toastText = "";
    private String[] destinationPaths = null;
    private String[] sourcePaths = null;
    private TypeFresh typeFresh = null;
    private boolean success;

    /**
     * Class constructor.
     * 
     * @param owner The owner of this copier and owner of the
     *                   ProgressDialog that we will use.
     */
    public FileCopier(TypeFresh owner) {
        typeFresh = owner;
    }

    @Override

    // params: String[] source, String[] destination, toastText 
    protected Void doInBackground(Object... params) {
        sourcePaths = (String[])params[0];
        destinationPaths = (String[])params[1];
        toastText = (String)params[2];

        Looper.prepare();
        String cmd = null;
        boolean needReboot = false;
        boolean remountRequired = destinationPaths[0].indexOf("/system/") == 0; 

        Process su = null;
        Runtime runtime = Runtime.getRuntime();
        success = false;

        if (remountRequired) { 
            try {
                if (!remount(READ_WRITE)) {
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
        }
            
        try {
            for (int i = 0; i < sourcePaths.length; i++) {
                if (sourcePaths[i].equals(destinationPaths[i])) {
                    continue;
                }
                publishProgress(sourcePaths[i]);
                su = runtime.exec("/system/bin/su");
                cmd = "cp -f " + sourcePaths[i] + " " + destinationPaths[i];
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
                    
                    success = false;
                    // even if there was an error, we want to continue to remount the system
                } else {
                    // If we've overwritten any of the core fonts, we need to reboot
                    if (destinationPaths[i].indexOf("/system/fonts/Droid") == 0) {
                        needReboot = true;
                    }
                    
                    success = true;
                }
                // clear up references, since we're done
                su.destroy();
            }

            if (needReboot) {
                publishProgress(TypeFresh.DIALOG_NEED_REBOOT);
            }
        } catch (IOException e) {
            Log.e(TypeFresh.TAG,e.toString());
            publishProgress(TypeFresh.DIALOG_NEED_ROOT);
        } catch (InterruptedException e) {
            Log.e(TypeFresh.TAG,e.toString());
            // hmm...how should I yell about this to the user?
        }
        return null;
    }


    @Override
    protected void onProgressUpdate(Object... message) {
        if (message[0] instanceof String) {
            // a String will just update the ProgressDialog
            typeFresh.progressDialog.setMessage((String)message[0]);
        } else {
            // otherwise we're calling another Dialog
            typeFresh.showDialog(((Number)message[0]).intValue());
        }
    }
    
    @Override
    protected void onPreExecute() {
        typeFresh.showDialog(TypeFresh.DIALOG_PROGRESS);
    }
    
    @Override
    protected void onPostExecute(Void result) {
        typeFresh.progressDialog.dismiss();
        if (success) {
            Toast.makeText(typeFresh, toastText, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Sets a new TypeFresh as this threads owner. This is important, since we
     * need a reference to the new Activity when the screen rotates.
     * 
     * @param owner The new Activity whose ProgressDialog to use.
     */
    public void setActivity(TypeFresh owner) {
        typeFresh = owner;
    }

    /**
     * Remounts /system read/write.
     * 
     * @param readwrite one of <code>READ_WRITE</code> or <code>READ_ONLY</code>.
     * 
     * @throws InterruptedException If our <code>su</code> process has a problem.
     * @throws IOException If our <code>su</code> process has a problem.
     * @return <code>boolean</code> of whether it succeeded.
     */
    public static boolean remount(int readwrite) throws IOException,InterruptedException {
        String type = (readwrite == READ_WRITE) ? "rw" : "ro";
        Process su = Runtime.getRuntime().exec("/system/bin/su");
        String systemBlock = systemLocation();

        Log.i(TypeFresh.TAG,"Remounting /system " + type);
        String cmd = "mount -o remount," + type + " " + systemBlock + " /system\nexit\n";
        Log.i(TypeFresh.TAG, "Executing :" + cmd);
        su.getOutputStream().write(cmd.getBytes());
        
        if (su.waitFor() != 0) {
            BufferedReader br
                    = new BufferedReader(new InputStreamReader(su.getErrorStream()), 200);
            String line;
            while((line = br.readLine()) != null) {
                Log.e(TypeFresh.TAG,"Error remounting: \"" + line + "\"");
            }
            Log.e(TypeFresh.TAG, "Could not remount, returning");
            return false;
        } else {
            Log.i(TypeFresh.TAG,"Remounted /system " + type);
        }
        return true;
    }

    /**
     * Locates the block on which /system is located and stores it in a static variable to ease
     * computation.
     * 
     * @throws InterruptedException If we have a problem running <code>mount</code>.
     * @throws IOException If we have a problem reading <code>stdout</code>.
     * @throws FileNotFoundException If we can't find <code>/system</code>.
     * @return <code>String</code> of the block name, or <code>null</code>.
     */
    protected static String systemLocation() throws InterruptedException, IOException, FileNotFoundException {
    	// no need to look for /system a second time
    	if (systemBlock != null) {
            return systemBlock;
        }

        Process su = Runtime.getRuntime().exec("/system/bin/su");
        String cmd = "mount\nexit\n";
        Log.d(TypeFresh.TAG, "running \"" + cmd + "\"");

        try {
            su.getOutputStream().write(cmd.getBytes());

            if (su.waitFor() != 0) {
	            BufferedReader br
	                    = new BufferedReader(new InputStreamReader(su.getErrorStream()), 200);
	            String line;
	            while((line = br.readLine()) != null) {
	                Log.e(TypeFresh.TAG,"Error: \"" + line + "\"");
	            }
	            Log.e(TypeFresh.TAG, "Could not find /system, returning");
	        } else {
	        	Log.d(TypeFresh.TAG, "Reading location");
	            BufferedReader br
	            		= new BufferedReader(new InputStreamReader(su.getInputStream()), 200);
	            String line;
	            
	            while ((line = br.readLine()) != null) {
	            	if (line.contains("/system")) {
			        	Log.d(TypeFresh.TAG, "line = " + line);
			            systemBlock = line.substring(0, line.indexOf(" "));
			            Log.i(TypeFresh.TAG,"Found /system mounted at " + systemBlock);

			            return systemBlock;
	            	}
	            }
	        }
        } catch (InterruptedException e) {
        	Log.e(TypeFresh.TAG, e.toString());
        	throw e;
        }

        throw new FileNotFoundException("Could not find /system");
    }

}
