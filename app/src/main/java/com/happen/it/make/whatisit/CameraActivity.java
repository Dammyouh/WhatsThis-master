package com.happen.it.make.whatisit;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import phototools.CameraInterface;
import util.DisplayUtil;


public class CameraActivity extends Activity implements CameraInterface.CamOpenOverCallback {
	private static final String TAG = "CameraActivity";
    private TextView resultTextView;
	public  Bitmap processedBitmap;
	private Bitmap bitmap;
	private Bitmap bitmap1;
    private String currentPhotoPath;
    private static final String PREF_USE_CAMERA_KEY = "USE_CAMERA";
    private SharedPreferences sharedPreferences;
	private Button indentify_b;
	public  ImageView img;
	private ImageView inputImage;
	CameraSurfaceView surfaceView = null;
	ImageButton shutterBtn;
	MaskView maskView = null;
	float previewRate = -1f;
	int DST_CENTER_RECT_WIDTH = 300;
	int DST_CENTER_RECT_HEIGHT = 300;
	Point rectPictureSize = null;
	CameraActivity mCameraActivity;
	private Uri imageUri;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_whats);
		initUI();
		initViewParams();
	}
	private void initUI(){
		surfaceView = (CameraSurfaceView)findViewById(R.id.camera_surfaceview);
		maskView = (MaskView)findViewById(R.id.view_mask);
		shutterBtn = (ImageButton)findViewById(R.id.btn_shutter);
		indentify_b = (Button)findViewById(R.id.identify_button);
		resultTextView = (TextView) findViewById(R.id.result_text);
		img = (ImageView) findViewById(R.id.pic);
		inputImage = (ImageView)findViewById(R.id.gallery);

		indentify_b.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (v != indentify_b) {
					return;
				}
				if (processedBitmap == null) {
					return;
				}
				new AsyncTask<Bitmap,Void, String>(){
					@Override
					protected void onPreExecute() {
						resultTextView.setVisibility(View.VISIBLE);
						resultTextView.setText("Calculating...");
					}
					@Override
					protected String doInBackground(Bitmap... bitmaps) {
						synchronized (indentify_b) {
							String tag = MxNetUtils.identifyImage(bitmaps[0]);
							return tag;
						}
					}
					@Override
					protected void onPostExecute(String tag) {
						resultTextView.setText(tag);
					}
				}.execute(processedBitmap);
			}
		});
		shutterBtn.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
				CameraInterface.getInstance().doTakePicture(CameraActivity.this);
			}
		});

        inputImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
					img.setVisibility(View.VISIBLE);
                    final Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent,Constants.SELECT_PHOTO_CODE);
            }
        });
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
		super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
		switch(requestCode) {
			case Constants.SELECT_PHOTO_CODE:
				if(resultCode == RESULT_OK){
					try {
						final Uri imageUri = imageReturnedIntent.getData();
						final InputStream imageStream = getContentResolver().openInputStream(imageUri);
						bitmap = BitmapFactory.decodeStream(imageStream);
						processedBitmap = processBitmap(bitmap);
						img.setImageBitmap(processedBitmap);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
				}
				break;
		}
	}
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    private void initViewParams(){
		LayoutParams params = surfaceView.getLayoutParams();
		Point p = DisplayUtil.getScreenMetrics(this);
		params.width = p.x;
		params.height = p.y;
		Log.i(TAG, "screen: w = " + p.x + " y = " + p.y);
		previewRate = DisplayUtil.getScreenRate(this);
		surfaceView.setLayoutParams(params);
		LayoutParams p2 = shutterBtn.getLayoutParams();
		p2.width = DisplayUtil.dip2px(this, 80);
		p2.height = DisplayUtil.dip2px(this, 80);;
		shutterBtn.setLayoutParams(p2);
	}
	@Override
	public void cameraHasOpened() {
		SurfaceHolder holder = surfaceView.getSurfaceHolder();
		CameraInterface.getInstance().doStartPreview(holder, previewRate);
		if(maskView != null){
			Rect screenCenterRect = createCenterScreenRect(DisplayUtil.dip2px(this, DST_CENTER_RECT_WIDTH)
					,DisplayUtil.dip2px(this, DST_CENTER_RECT_HEIGHT));
			maskView.setCenterRect(screenCenterRect);
		}
	}
	public void updatePicture(Bitmap pic) {
		ImageView img = (ImageView) findViewById(R.id.pic);
		if(img != null) {
			img.setImageBitmap(pic);
			img.setVisibility(View.VISIBLE);
		}
	}
//	private Point createCenterPictureRect(int w, int h){
//		int wScreen = DisplayUtil.getScreenMetrics(this).x;
//		int hScreen = DisplayUtil.getScreenMetrics(this).y;
//		int wSavePicture = CameraInterface.getInstance().doGetPrictureSize().y;
//		int hSavePicture = CameraInterface.getInstance().doGetPrictureSize().x;
//		float wRate = (float)(wSavePicture) / (float)(wScreen);
//		float hRate = (float)(hSavePicture) / (float)(hScreen);
//		float rate = (wRate <= hRate) ? wRate : hRate;
//		int wRectPicture = (int)( w * wRate);
//		int hRectPicture = (int)( h * hRate);
//		return new Point(wRectPicture, hRectPicture);
//	}
	private Rect createCenterScreenRect(int w, int h){
		int x1 = DisplayUtil.getScreenMetrics(this).x / 2 - w / 2;
		int y1 = DisplayUtil.getScreenMetrics(this).y / 2 - h / 2;
		int x2 = x1 + w;
		int y2 = y1 + h;
		return new Rect(x1, y1, x2, y2);
	}

	@Override
	protected void onResume() {
		super.onResume();

		//TODO  Replace with AsyncTask
		Thread openThread = new Thread(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				CameraInterface.getInstance().doOpenCamera(CameraActivity.this);
			}
		};
		openThread.start();
	}

		static final int SHORTER_SIDE = 256 ;//256
		static final int DESIRED_SIDE = 224;//224

	public  Bitmap processBitmap(final Bitmap origin) {
		//TODO: error handling
		final int originWidth = origin.getWidth();
		final int originHeight = origin.getHeight();
		int height = SHORTER_SIDE;
		int width = SHORTER_SIDE;
		if (originWidth < originHeight) {
			height = (int)((float)originHeight / originWidth * width);
		} else {
			width = (int)((float)originWidth / originHeight * height);
		}
		final Bitmap scaled = Bitmap.createScaledBitmap(origin, width, height, false);
		int y = (height - DESIRED_SIDE)/2 ;
		int x = (width - DESIRED_SIDE)/2;
		return Bitmap.createBitmap(scaled, x, y, DESIRED_SIDE, DESIRED_SIDE);
	}
}
