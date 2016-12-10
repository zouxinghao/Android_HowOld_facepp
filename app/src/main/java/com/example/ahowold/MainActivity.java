package com.example.ahowold;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.ahowold.Faceppdetect.CallBack;
import com.facepp.error.FaceppParseException;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.app.Activity;
import android.app.DownloadManager.Request;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {

	
	private static final int PICK_CODE = 0x110;
	private ImageView mPhoto;
	private Button mGetImage;
	private Button mDetect;
	private TextView mTip;
	private View mWaiting;
	private String mCurrentPhotoStr;//当前照片的路径
	private Bitmap mPhotoImage;
	private Paint mPaint;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		initViews();
		
		initEvents();
		mPaint=new Paint();
	}
	
	private void initEvents() {
		mGetImage.setOnClickListener(this);
		mDetect.setOnClickListener(this);
		
	}

	private void initViews(){
		mPhoto=(ImageView) findViewById(R.id.id_photo);
		mGetImage=(Button) findViewById(R.id.id_get);
		mDetect=(Button) findViewById(R.id.id_detect);
		mTip=(TextView) findViewById(R.id.id_tip);
		mWaiting=findViewById(R.id.id_waiting);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {


		if(requestCode==PICK_CODE){
			if (intent!=null) {
				Uri uri=intent.getData();
				Cursor cursor=getContentResolver().query(uri, null, null, null, null);
				cursor.moveToFirst();
				//选择、得到照片的路径
				int idx=cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
				mCurrentPhotoStr=cursor.getString(idx);
				cursor.close();
				
				//对照片的尺寸（大小）进行压缩
				resizephoto();
				
				mPhoto.setImageBitmap(mPhotoImage);
				mTip.setText("Click Detect==>");
			}
		}
		
		super.onActivityResult(requestCode, resultCode, intent);
	}
	
	private void resizephoto() {
		BitmapFactory.Options options=new BitmapFactory.Options();
		options.inJustDecodeBounds=true;
		
		BitmapFactory.decodeFile(mCurrentPhotoStr,options);
		
		double ratio =Math.max(options.outWidth*1.0d/1024f,options.outHeight*1.0d/1024f);
		options.inSampleSize=(int) Math.ceil(ratio);
		options.inJustDecodeBounds=false;
		mPhotoImage=BitmapFactory.decodeFile(mCurrentPhotoStr,options);
	}

	
	private static final int MSG_SUCCESS=0x111;
	private static final int MSG_ERROR=0x112;
	
	private Handler mHandler=new Handler(){
		@Override
		public void handleMessage(android.os.Message msg) {
			
			switch (msg.what) {
			case MSG_SUCCESS:
				mWaiting.setVisibility(View.GONE);
				JSONObject rs=(JSONObject) msg.obj;
				prepareResultBitmap(rs);
				
				mPhoto.setImageBitmap(mPhotoImage);
				break;
			case MSG_ERROR:
				mWaiting.setVisibility(View.GONE);
				String errorMsg=(String) msg.obj;
				
				if(TextUtils.isDigitsOnly(errorMsg)){
					mTip.setText("Error.");
				}else{
					mTip.setText(errorMsg);
				}
				break;
			}
			super.handleMessage(msg);
		}
	};
	
	@Override
	public void onClick(View v){
		switch (v.getId()) {
		case R.id.id_get:
			
			Intent intent=new Intent(Intent.ACTION_PICK);
			intent.setType("image/*");
			startActivityForResult(intent, PICK_CODE);
			break;
		case R.id.id_detect:
			
			mWaiting.setVisibility(View.VISIBLE);
			if(mCurrentPhotoStr!=null&&!mCurrentPhotoStr.trim().equals("")){
				resizephoto();
			}else{
				mPhotoImage=BitmapFactory.decodeResource(getResources(), R.drawable.t4);
			}
			Faceppdetect.detect(mPhotoImage, new CallBack() {
				
				@Override
				public void success(JSONObject result) {
					Message msg=Message.obtain();
					msg.what=MSG_SUCCESS;
					msg.obj=result;
					mHandler.sendMessage(msg);
					
				}
				
				@Override
				public void error(FaceppParseException exception) {
					Message msg=Message.obtain();
					msg.what=MSG_ERROR;
					msg.obj=exception.getErrorMessage();
					mHandler.sendMessage(msg);
					
				}
			});
			break;
		}
	}

	protected void prepareResultBitmap(JSONObject rs) {
		Bitmap bitmap=Bitmap.createBitmap(mPhotoImage.getWidth(), mPhotoImage.getHeight(), mPhotoImage.getConfig());
		Canvas canvas =new Canvas(bitmap);
		canvas.drawBitmap(mPhotoImage,0,0,null);
		
		try {
			JSONArray faces=rs.getJSONArray("face");
			int faceCount=faces.length();
			mTip.setText("find"+faceCount);
			
			for (int i = 0; i < faceCount; i++) {
				//拿到face对象
				JSONObject face=faces.getJSONObject(i);
				JSONObject posObj=face.getJSONObject("position");
			
				float x=(float) posObj.getJSONObject("center").getDouble("x");
				float y=(float) posObj.getJSONObject("center").getDouble("y");
			
				float w=(float) posObj.getDouble("width");
				float h=(float) posObj.getDouble("height");
				
				x=x/100*bitmap.getWidth();
				y=y/100*bitmap.getHeight();
				
				w=w/100*bitmap.getWidth();
				h=h/100*bitmap.getHeight();
				
				mPaint.setColor(0xffffffff);
				mPaint.setStrokeWidth(3);
				
				//画出脸部的框图
				canvas.drawLine(x-w/2, y-h/2, x-w/2, y+h/2, mPaint);
				canvas.drawLine(x-w/2, y-h/2, x+w/2, y-h/2, mPaint);
				canvas.drawLine(x+w/2, y-h/2, x+w/2, y+h/2, mPaint);
				canvas.drawLine(x-w/2, y+h/2, x+w/2, y+h/2, mPaint);
				//获得年龄和性别
				int age=face.getJSONObject("attribute").getJSONObject("age").getInt("value");
				String gender=face.getJSONObject("attribute").getJSONObject("gender").getString("value");
			
				Bitmap messagebBitmap=buildmessageBitmap(age,"male".equals(gender));
			
				//缩放的代码，可以使气泡的大小与所选图片的大小相匹配
				int msgWidth=messagebBitmap.getWidth();
				int msgHeight=messagebBitmap.getHeight();
				
				if(bitmap.getWidth()<mPhoto.getWidth()&&bitmap.getHeight()<mPhoto.getHeight()){
					//确定一个缩放的比例
					float ratio=Math.max(bitmap.getWidth()*1.0f/mPhoto.getWidth(), bitmap.getHeight()*1.0f/mPhoto.getHeight());
					messagebBitmap=Bitmap.createScaledBitmap(messagebBitmap, (int)(msgWidth*ratio),(int) (msgHeight*ratio), false);
				
				}
				
				canvas.drawBitmap(messagebBitmap, x-messagebBitmap.getWidth()/2, y-h/2-messagebBitmap.getHeight(), null);
				mPhotoImage=bitmap;
				
				}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Bitmap buildmessageBitmap(int age, boolean ismale) {
		TextView tv=(TextView) mWaiting.findViewById(R.id.id_a_and_g);
		tv.setText(age+"");
		if (ismale) {
			tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.male), null, null, null);
		}else{
			tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.female), null, null, null);
		}
		
		tv.setDrawingCacheEnabled(true);
		Bitmap bitmap=Bitmap.createBitmap(tv.getDrawingCache());
		tv.destroyDrawingCache();
		
		return bitmap;
	}

}
	

		

