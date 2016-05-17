package com.bnsantos.video.recording;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.netcompss.ffmpeg4android.GeneralUtils;
import com.netcompss.loader.LoadJNI;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by bruno on 17/05/16.
 */
public class CompressAsyncTask extends AsyncTask<Uri, Void, Uri> {
  private static final String TAG = CompressAsyncTask.class.getSimpleName();
  private WeakReference<MainActivity> mWeakReference;
  private String mFolder;
  private String mFilename;
  private long mStartTime;
  private long mEndTime;

  public CompressAsyncTask(MainActivity context) {
    mWeakReference = new WeakReference<>(context);

    File mediaFolder = Environment.getExternalStoragePublicDirectory("VideoCompression");
    if(!mediaFolder.exists()){
      Log.i(TAG, "Creating [" + mediaFolder.getAbsolutePath() + "] folders " + mediaFolder.mkdirs());
    }
    mFolder = mediaFolder.getAbsolutePath() + "/";
    mFilename = "VID-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(new Date());
  }

  @Override
  protected Uri doInBackground(Uri... uris) {
    mStartTime = System.currentTimeMillis();

    String input = getRealPath(uris[0]);
    String output = mFolder + mFilename + ".mp4";
    LoadJNI vk = new LoadJNI();
    try {
      String commandStr = "ffmpeg -y -i " + input + " -strict -2 -s 480x270 " + output;
      Log.i(TAG, commandStr);
      vk.run(GeneralUtils.utilConvertToComplex(commandStr), mFolder, mWeakReference.get());
      Log.i(TAG, "ffmpeg4android finished successfully");
      mEndTime = System.currentTimeMillis();
      long difference = (long) ((mEndTime - mStartTime)/1000.0);
      Log.i(TAG, "Time spent: " + difference + " s");
    } catch (Throwable e) {
      Log.e(TAG, "vk run exception.", e);
    }

    return Uri.fromFile(new File(output));
  }

  @Override
  protected void onPostExecute(Uri uri) {
    super.onPostExecute(uri);
    Log.i(TAG, "Total time spent: " + (mEndTime-mStartTime));
    mWeakReference.get().compressFinished(uri, mEndTime-mStartTime);
    Toast.makeText(mWeakReference.get(), "Finished", Toast.LENGTH_LONG).show();
  }

  @SuppressLint("NewApi")
  private String getRealPath(Uri uri) {
    final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

    // DocumentProvider
    if (isKitKat && DocumentsContract.isDocumentUri(mWeakReference.get(), uri)) {
      if (isExternalStorageDocument(uri)) { // ExternalStorageProvider
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        if ("primary".equalsIgnoreCase(type)) {
          return Environment.getExternalStorageDirectory() + "/" + split[1];
        }else {
          Toast.makeText(mWeakReference.get(), "TODO", Toast.LENGTH_SHORT).show();
          /*//Below logic is how External Storage provider build URI for documents
          StorageManager mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
          final StorageVolume[] volumes = mStorageManager.getVolumeList();
          for (StorageVolume volume : volumes) {
            final boolean mounted = Environment.MEDIA_MOUNTED.equals(volume.getState()) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(volume.getState());
            //if the media is not mounted, we need not get the volume details
            if (!mounted) continue;
            //Primary storage is already handled.
            if (volume.isPrimary() && volume.isEmulated()) continue;
            //Build the actual path based on the uuid
            if (volume.getUuid() != null && volume.getUuid().equals(type)) {
              return volume.getPath() + "/" +split[1];
            }
          }*/
        }
      }
      else if (isDownloadsDocument(uri)) {  // DownloadsProvider
        final String id = DocumentsContract.getDocumentId(uri);
        final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

        return getDataColumn(mWeakReference.get(), contentUri, null, null);
      }
      else if (isMediaDocument(uri)) {  // MediaProvider
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        Uri contentUri = null;
        if ("image".equals(type)) {
          contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if ("video".equals(type)) {
          contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if ("audio".equals(type)) {
          contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        final String selection = "_id=?";
        final String[] selectionArgs = new String[] { split[1] };

        return getDataColumn(mWeakReference.get(), contentUri, selection, selectionArgs);
      }
    }
    else if ("content".equalsIgnoreCase(uri.getScheme())) { // MediaStore (and general)
      return getDataColumn(mWeakReference.get(), uri, null, null);
    }
    else if ("file".equalsIgnoreCase(uri.getScheme())) {  // File
      return uri.getPath();
    }

    return null;
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is ExternalStorageProvider.
   */
  public boolean isExternalStorageDocument(Uri uri) {
    return "com.android.externalstorage.documents".equals(uri.getAuthority());
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is DownloadsProvider.
   */
  public boolean isDownloadsDocument(Uri uri) {
    return "com.android.providers.downloads.documents".equals(uri.getAuthority());
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is MediaProvider.
   */
  public boolean isMediaDocument(Uri uri) {
    return "com.android.providers.media.documents".equals(uri.getAuthority());
  }

  /**
   * Get the value of the data column for this Uri. This is useful for
   * MediaStore Uris, and other file-based ContentProviders.
   *
   * @param context The context.
   * @param uri The Uri to query.
   * @param selection (Optional) Filter used in the query.
   * @param selectionArgs (Optional) Selection arguments used in the query.
   * @return The value of the _data column, which is typically a file path.
   */
  public String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
    Cursor cursor = null;
    final String column = "_data";
    final String[] projection = { column };

    try {
      cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
      if (cursor != null && cursor.moveToFirst()) {
        final int column_index = cursor.getColumnIndexOrThrow(column);
        return cursor.getString(column_index);
      }
    } finally {
      if (cursor != null)
        cursor.close();
    }
    return null;
  }
}
