package eu.lighthouselabs.obd.reader.activity;

import java.util.ArrayList;

import eu.lighthouselabs.obd.reader.R;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

public class DialView extends View {

	Bitmap bitmapOrg;
	int width, height, rpm, rpmMax=8192;
	int RPMperGrad = rpmMax/12;
	ArrayList<Float> preAngle;
	ImageView img = (ImageView) findViewById(R.id.tick);
	float angle;
	
	public DialView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		preAngle = new ArrayList<Float>();
		
		if(!isInEditMode()){
			bitmapOrg = BitmapFactory.decodeResource(getResources(),R.drawable.tick).copy(Bitmap.Config.ARGB_8888,  true );
			width = bitmapOrg.getWidth();
			height = bitmapOrg.getHeight();			
		} 
	}
	
	
	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		angle = (float) ( this.rpm / RPMperGrad * 15 );		
		//Log.d("angle", angle+"");
		
		float fromAngle = 0;
		if (preAngle.size()>2){
			fromAngle =  preAngle.get(preAngle.size()-2);
		}
		
		/*canvas.translate(200,200);	
		canvas.save();
		canvas.rotate(angle, width/2, height/2);
		canvas.drawBitmap(bitmapOrg, 0, 0, null);
		canvas.restore();*/
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Animation anim = new RotateAnimation(fromAngle, angle);  
        //anim.setInterpolator(new AccelerateDecelerateInterpolator());  
        anim.setDuration(10);  
        img.setAnimation(anim);
        anim.startNow();
	    
	}
	
	public void setRPM(int progress){		
		this.rpm = progress;				
	}
	
	public void setpreAngle(float preAngle){
		this.preAngle.add(preAngle / RPMperGrad * 15);		
	}
	
}