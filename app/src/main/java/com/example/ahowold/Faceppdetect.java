package com.example.ahowold;

import java.io.ByteArrayOutputStream;

import org.apache.http.HttpRequest;
import org.json.JSONObject;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import android.graphics.Bitmap;
import android.util.Log;

public class Faceppdetect {
	
	public interface CallBack{
		void success(JSONObject result);
		void error(FaceppParseException exception);
	}
	
	
	public static void detect(final Bitmap bm,final CallBack callBack){
		
		new Thread(new Runnable() {
			@Override//这里需要吗？
			public void run() {
				
				
				try {
					//创建请求 
					HttpRequests request=new HttpRequests(Constant.KEY, Constant.SCERET, true, true);
				
					Bitmap bmsmall=Bitmap.createBitmap(bm,0,0,bm.getWidth(),bm.getHeight());
					ByteArrayOutputStream stream=new ByteArrayOutputStream();
					bmsmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);
					byte[] arrays=stream.toByteArray();
				
					PostParameters params=new PostParameters();
					params.setImg(arrays);
					JSONObject jsonObject=request.detectionDetect(params);
					//为JsonObject打一个log
					Log.e("TAG", jsonObject.toString());
					if(callBack!=null){
						callBack.success(jsonObject);
					}
				} catch (FaceppParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					if(callBack!=null){
						callBack.error(e);
					}
				}
			}
		}).start();
	}
}
