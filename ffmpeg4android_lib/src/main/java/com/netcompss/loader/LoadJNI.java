package com.netcompss.loader;

import android.content.Context;
import android.util.Log;

import com.netcompss.ffmpeg4android.CommandValidationException;
import com.netcompss.ffmpeg4android.GeneralUtils;
import com.netcompss.ffmpeg4android.Prefs;

public final class LoadJNI {

	static {
		System.loadLibrary("loader-jni");
	}
	
	/**
	 * 
	 * @param args ffmpeg command
	 * @param workFolder working directory 
	 * @param ctx Android context
	 * @param isValidate apply validation to the command
	 * @throws CommandValidationException
	 */
	public void run(String[] args, String workFolder, Context ctx, boolean isValidate) throws CommandValidationException {
		Log.i(Prefs.TAG, "running ffmpeg4android_lib: " + Prefs.version);
		// delete previous log: this is essential for correct progress calculation
		String vkLogPath = workFolder + "vk.log";
		GeneralUtils.deleteFileUtil(vkLogPath);
		GeneralUtils.printCommand(args);
		if (isValidate) {
			if (GeneralUtils.isValidCommand(args))
				load(args, workFolder, getVideokitLibPath(ctx), true);
			else
				throw new CommandValidationException();
		}
		else {
			load(args, workFolder, getVideokitLibPath(ctx), true);
		}
	}
	
	/**
	 * 
	 * @param args ffmpeg command
	 * @param videokitSdcardPath working directory 
	 * @param ctx Android context
	 * @throws CommandValidationException
	 */
	public void run(String[] args, String workFolder, Context ctx) throws CommandValidationException {
		run(args, workFolder, ctx, true);
	}
	
	private static String getVideokitLibPath(Context ctx) {
		String videokitLibPath = ctx.getFilesDir().getParent()  + "/lib/libvideokit.so";
		
		//String videokitLibPath = ctx.getFilesDir().getParent()  + "/lib/arm64/libvideokit.so";
		
		// only this works on Android M, and the number changes (demo-2, demo-1)
		//String videokitLibPath = "/data/app/com.examples.ffmpeg4android_demo-1/lib/arm64/libvideokit.so";
		
		
		Log.i(Prefs.TAG, "videokitLibPath: " + videokitLibPath);
		return videokitLibPath;
		
	}
	
	public void fExit( Context ctx) {
		fexit(getVideokitLibPath(ctx));
	}
	
	public native String fexit(String videokitLibPath);
	
	public native String unload();

	public native String load(String[] args, String videokitSdcardPath, String videokitLibPath, boolean isComplex);
}
