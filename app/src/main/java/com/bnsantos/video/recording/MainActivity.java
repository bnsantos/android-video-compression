package com.bnsantos.video.recording;

import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.bnsantos.video.recording.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
  private static final String BUNDLE_LENGTH = "BUNDLE_VIDEO_LENGTH";
  private static final String BUNDLE_SIZE = "BUNDLE_VIDEO_SIZE";
  private static final String BUNDLE_QUALITY = "BUNDLE_VIDEO_QUALITY";
  private static final int ACTION_RECORD_VIDEO = 3;
  private int mMaxLength = 60;
  private int mQuality = 0;
  private long mMaxSize = 5; //In Mb
  private static final long K = 1024;

  private Uri mVideoUri;

  private ActivityMainBinding binding;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

    setSupportActionBar(binding.toolbar);
    binding.fab.setOnClickListener(this);

    updateUI();
  }

  @Override
  public void onClick(View view) {
    mVideoUri = null;
    record();
  }

  private void record(){
    updateValues();

    Intent record = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
    record.putExtra(MediaStore.EXTRA_DURATION_LIMIT, mMaxLength);
    record.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, mQuality );
    record.putExtra(MediaStore.EXTRA_SIZE_LIMIT, mMaxSize * K * K);
    if(record.resolveActivity(getPackageManager())!=null){
      startActivityForResult(record, ACTION_RECORD_VIDEO);
    }else{
      Toast.makeText(MainActivity.this, R.string.no_video_app, Toast.LENGTH_SHORT).show();
    }
  }

  private void updateUI(){
    binding.maxLength.setText(Integer.toString(mMaxLength));
    binding.maxSize.setText(Long.toString(mMaxSize));
    binding.quality.setChecked(mQuality==1);

    if(mVideoUri!=null){
      binding.resultLayout.setVisibility(View.VISIBLE);


      Cursor returnCursor = getContentResolver().query(mVideoUri, null, null, null, null);
    /*
     * Get the column indexes of the data in the Cursor,
     * move to the first row in the Cursor, get the data,
     * and display it.
     */
      int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
      int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
      returnCursor.moveToFirst();
      binding.path.setText(getString(R.string.path, returnCursor.getString(nameIndex)));
      binding.size.setText(getString(R.string.size, Long.toString(returnCursor.getLong(sizeIndex)/K)));

      MediaMetadataRetriever retriever = new MediaMetadataRetriever();
      retriever.setDataSource(this, mVideoUri);
      String length = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
      binding.length.setText(getString(R.string.length, length));

      String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
      String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
      int rotation = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
      if(rotation==90 || rotation==180){
        binding.resolution.setText(getString(R.string.resolution, height + "x" + width));
      }else{
        binding.resolution.setText(getString(R.string.resolution, width + "x" + height));
      }
    }else{
      binding.resultLayout.setVisibility(View.GONE);
    }
  }

  private void updateValues(){
    mMaxLength = Integer.parseInt(binding.maxLength.getText().toString());
    mMaxSize = Long.parseLong(binding.maxSize.getText().toString());
    mQuality = binding.quality.isChecked()?1:0;
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(BUNDLE_LENGTH, mMaxLength);
    outState.putLong(BUNDLE_SIZE, mMaxSize);
    outState.putInt(BUNDLE_QUALITY, mQuality);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);

    if (savedInstanceState!=null) {
      mMaxLength = savedInstanceState.getInt(BUNDLE_LENGTH, mMaxLength);
      mMaxSize = savedInstanceState.getLong(BUNDLE_SIZE, mMaxSize);
      mQuality = savedInstanceState.getInt(BUNDLE_QUALITY, mQuality);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if(resultCode == RESULT_OK && requestCode == ACTION_RECORD_VIDEO){
      if (data!=null) {
        mVideoUri = data.getData();
        updateUI();
      }
    }
  }
}
