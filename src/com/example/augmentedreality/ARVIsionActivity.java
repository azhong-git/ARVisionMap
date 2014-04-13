package com.example.augmentedreality;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.detection.DetectionBasedTracker;
import com.example.openglbasics.R;

public class ARVIsionActivity extends Activity implements CvCameraViewListener2, SensorEventListener, GestureDetector.OnGestureListener,
GestureDetector.OnDoubleTapListener {
	// OpenGL content view
	private GLSurfaceView glSurfaceView;
	private boolean rendererSet = false;
	private ARVisionRenderer renderer;
		
	// Camera view
	private CameraBridgeViewBase mCameraView;
	// Back camera
	private int mCameraIndex = 0;
	
	// OpenCV objection detection
	private static final Scalar FONT_COLOR = new Scalar (0, 76, 253, 10);
	private static final Scalar MENU_COLOR = new Scalar (255, 0, 125, 100);
	
	private Mat mRgba;
	private Mat mGray;
	private File mCascadeFile;
	private static final String TAG = "ARVision::Activity";
    private int x3, midx, xx;
    private int y3, midy;
	
	// OpenGL layout
	FrameLayout view;
	// sensor layout
	FrameLayout topview;	
	// sensor view
    TextView mOrientationData;
    // location layout
    FrameLayout locationview;
    // location view
    TextView mLocationData;
    
    // menus
    private MenuItem locationSet;
    private int locationType = 0;
    private String [] locationSetOrNot = new String [2];
    
    private MenuItem touchEnable;
    private int touchType = 0;
    private String [] touchSetOrNot = new String [2];
    private String note = "no action";
    private static final String DEBUG_TAG = "Gestures";
    private GestureDetectorCompat mDetector; 
    
    private MenuItem moveSelect;
    private int moveType = 0;
    private String [] moveCameraOrObject = new String [2];
    
	// sensors
    private SensorManager mSensMan;
    static public float mAzimuth;
    static public float roll;
    static public float pitch;

    private float[] mGravs = new float[3];
    private float[] mGeoMags = new float[3];
    public static float[] mOrientation = new float[3];
    public static float[] mRotationM = new float[16];
    public static float[] mRemapedRotationM = new float[16];
    public static boolean sensorAvailable = false;
    public Timer timer;
    public long timerInterval = 5000l;
    
    private boolean mFailed;
    
	private final float ALPHA = 0.15f;

	// home center
	double lat = 37.87565;
	double lon = -122.25869;
	public static int METER_PER_LAT = 110994;
	public static int METER_PER_LON = 87980;
	double dx = 0.0;
	double dy = 0.0;
	double dz = 0.0;
	// if true: initial map loaded relatively, if false use lat and lon as defined
	boolean fetchLocationFirstAttempt = false;
	private String prevLoc = "";
	
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    System.loadLibrary("detection_based_tracker");
                	try {
                		InputStream is = getResources().openRawResource(R.raw.surf10_80_40);
                		File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                		mCascadeFile = new File(cascadeDir, "surf10_80_40.xml");
                		FileOutputStream os = new FileOutputStream(mCascadeFile);
                		
                		byte [] buffer = new byte [4096];
                		int bytesRead;
                		while ((bytesRead = is.read(buffer)) != -1) {
                			os.write(buffer, 0, bytesRead);
                		}
                		is.close();
                		os.close();
                	 
                		new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);
                		cascadeDir.delete();
                	}
                	catch (IOException e){
                		Log.e(TAG, "Cannot load cascade, IOException!");
                	}
                    mCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };	
	  
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // keep screen on and set the layout
        locationSetOrNot[0] = "Enable location";
        locationSetOrNot[1] = "Disable location";
        touchSetOrNot[0] = "Enable touch";
        touchSetOrNot[1] = "Disable touch";
        moveCameraOrObject[0] = "Move Object";
        moveCameraOrObject[1] = "Move Camera";

        mDetector = new GestureDetectorCompat(this,this);
        mDetector.setOnDoubleTapListener(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        view = (FrameLayout) findViewById(R.id.camera_preview);           
        topview = (FrameLayout) findViewById(R.id.topview);
        locationview = (FrameLayout) findViewById(R.id.locationview);
        
        initCamera();


        mOrientationData = new TextView(this); // add the sensor orientation data
        topview.addView(mOrientationData);

        mLocationData = new TextView(this);
        locationview.addView(mLocationData);

        // Initiate the Sensor Manager and register this as Listener for the required sensor types:
		// TODO: Find how to get a SensorManager outside an Activity, to implement as a utility class.
        mSensMan = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensMan.registerListener(this,
        		mSensMan.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), //Anonymous Sensors- no further use for them.
                SensorManager.SENSOR_DELAY_GAME);
        mSensMan.registerListener(this,
        		mSensMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME); // TODO to change it to SENSOR_DELAY_NORMAL

        initGl();  // initialize OpenGL view
    }

    @Override
    protected void onPause() {
    	super.onPause();
    	if (rendererSet) {
    		glSurfaceView.onPause();
    	}
        if (mCameraView != null)
            mCameraView.disableView();
    	
    }

    @Override
    protected void onResume() {
    	super.onResume();
    	if (rendererSet) {
    		glSurfaceView.onResume();
    	}
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }
    
    public void onDestroy() {
        super.onDestroy();
    	if (rendererSet) {
    		glSurfaceView.onPause();
    	}
        if (mCameraView != null)
            mCameraView.disableView();
    }
    

    
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
        xx = 100;
        x3 = 400;
        y3 = 240;
        midx = 400;
        midy = 240;
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	mRgba = inputFrame.rgba();
    	mGray = inputFrame.gray();
    	if (touchType == 1) {
    		if (moveType == 0)
    			Core.putText(mRgba, "Move camera", new Point(midx-x3, midy+y3), 0, 1.25, MENU_COLOR, 2);
    		else
    			Core.putText(mRgba, "Move object", new Point(midx-x3, midy+y3), 0, 1.25, MENU_COLOR, 2);	
    	}
    	
        if (ARVisionRenderer.GLStatus == ARVisionRenderer.GraphicsStatus.Loading)
        	Core.putText(mRgba, "Loading...", new Point(midx-xx, midy), 0, 1.5, FONT_COLOR, 2);
        return mRgba;
    }
    
    
    private void initCamera() {
    	mCameraView = new JavaCameraView(this, mCameraIndex);
    	mCameraView.setCvCameraViewListener(this);
//    	mCameraView.setMaxFrameSize(640,480);
    	view.addView(mCameraView);
    }
    
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
        	case Sensor.TYPE_ACCELEROMETER:
        		mGravs = lowPass(event.values.clone(), mGravs);
                break;
        	case Sensor.TYPE_MAGNETIC_FIELD:
                mGeoMags = lowPass(event.values.clone(), mGeoMags);
        	default:
        		return;
        }

        if (SensorManager.getRotationMatrix(mRotationM, null, mGravs,mGeoMags)){
        		//Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        		//final int rotation = display.getRotation();
        		{
        			SensorManager.remapCoordinateSystem(mRotationM, SensorManager.AXIS_X,
        					SensorManager.AXIS_Z, mRemapedRotationM);
        		}
        		/*else if (rotation == Surface.ROTATION_90) {
        			SensorManager.remapCoordinateSystem(mRotationM, SensorManager.AXIS_Z,
        					SensorManager.AXIS_MINUS_Y, mRemapedRotationM);
        		}*/
        		SensorManager.getOrientation(mRemapedRotationM, mOrientation);
                onSuccess();
                if (rendererSet) {
                	glSurfaceView.queueEvent(new Runnable() {
                		@Override
                		public void run () {
                			if (locationType == 0)
                				renderer.sensorUpdate(mAzimuth, roll, pitch, 0, 0, 0);
                			else
                				renderer.sensorUpdate(mAzimuth, roll, pitch, dx, dy, dz);
                		}
                	});
                }
        }
        else onFailure();
    }

    void onSuccess(){
        if (mFailed) mFailed = false;
        // Convert the azimuth to degrees in 0.5 degree resolution.
        mAzimuth = (float) Math.round((Math.toDegrees(mOrientation[0])) *2)/2;
        // Adjust the range: 0 < range <= 360 (from: -180 < range <= 180).
        mAzimuth = (mAzimuth+360)%360; // alternative: mAzimuth = mAzimuth>=0 ? mAzimuth : mAzimuth+360;
        pitch = (float) Math.round((Math.toDegrees(mOrientation[1])) *2)/2;
        roll = (float) Math.round((Math.toDegrees(mOrientation[2])) *2)/2;
        mOrientationData.setText("Azimuth= " + mAzimuth + " Pitch=" + pitch + " Roll=" + roll + "; Action: " + note);
        sensorAvailable = true;
	}

    private float [] lowPass(float [] in, float [] out) {
    	if (out == null)
    		return in;
    	for (int i = 0; i < in.length; i++) {
    		out[i] = out[i] + ALPHA * (in[i] - out[i]);
    	}
    	return out;
    	
    }
    
	void onFailure() {
        if (!mFailed) {
        	mFailed = true;
            mOrientationData.setText("Failed to retrive rotation Matrix");
            //sensorAvailable = false;
        }
	}
	
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	locationSet = menu.add(locationSetOrNot[locationType]);
    	touchEnable = menu.add(touchSetOrNot[touchType]);
    	moveSelect = menu.add(moveCameraOrObject[moveType]);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item == locationSet) {
    		locationType = (locationType + 1) % 2;
    		if (locationType == 1) {
    			timer = new Timer();
    			timer.schedule(task, 0l, timerInterval);
    		}
    		item.setTitle(locationSetOrNot[locationType]);
    	}
    	else if (item == touchEnable) {
    		touchType = (touchType + 1) % 2;
    		item.setTitle(touchSetOrNot[touchType]);
    	}
    	else if (item == moveSelect) {
    		moveType = (moveType + 1) % 2;
    		item.setTitle(moveCameraOrObject[moveType]);
    	}
    	return true;
    }
    
	public DisplayMetrics getDimensions(){
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		return metrics;
	} 
    
    private void initGl() {
        renderer = new ARVisionRenderer(this);
        glSurfaceView = new GLSurfaceView(this);
        final boolean supportsEs2 = true;
        if (supportsEs2) {
        	glSurfaceView.setEGLContextClientVersion(2);
        	glSurfaceView.setZOrderMediaOverlay(true);
        	glSurfaceView.setEGLConfigChooser(8,8,8,8,16,0);
        	glSurfaceView.setRenderer(renderer);
        	glSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        	rendererSet = true;
        }
        else {
        	Toast.makeText(this, "This device does not support OpenGL ES 2.0.",
        			Toast.LENGTH_LONG).show();
        	return;
        }
        
        view.addView(glSurfaceView);
    }

	@Override 
    public boolean onTouchEvent(MotionEvent event){ 
        this.mDetector.onTouchEvent(event);
        
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent event) { 
        Log.d(DEBUG_TAG,"onDown: " + event.toString()); 
        if (touchType == 0)
        	note = "touch disabled";
        else
        	note = "down";
        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, 
            final float velocityX, final float velocityY) {
        Log.d(DEBUG_TAG, "onFling: " + event1.toString()+event2.toString());
        if (touchType == 0)
        	note = "touch disabled";
        else {
        	note = "fling" + " X: "+ velocityX + " Y: " + velocityY;
        	if (rendererSet) {
            	glSurfaceView.queueEvent(new Runnable() {
            		@Override
            		public void run () {	            			
                		renderer.moveCamera(velocityX/8000.0f, velocityY/8000.0f);
            		}
            	});
            }
        	
        }
        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        Log.d(DEBUG_TAG, "onLongPress: " + event.toString()); 
        if (touchType == 0)
        	note = "touch disabled";
        else {
        	note = "long press";
        	if (rendererSet) {
        		final float normalizedX = (event.getX() / (float) getDimensions().widthPixels) * 2- 1;
        		final float normalizedY = -((event.getY() / (float) getDimensions().heightPixels) * 2 -1);
        		note = note + " X: " + normalizedX + " Y: " + normalizedY;
            	glSurfaceView.queueEvent(new Runnable() {
            		@Override
            		public void run () {	            			
            			renderer.setObject(0, (float) ((mAzimuth - normalizedX)%360), -pitch+normalizedY*5f, roll);
            		}
            	});
            }	        	
        }
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, final float distanceX,
            final float distanceY) {
        Log.d(DEBUG_TAG, "onScroll: " + e1.toString()+e2.toString());
        if (touchType == 0)
        	note = "touch disabled";
        else {
        	note = "scroll" + " X: "+distanceX + " Y: "+distanceY;

        }
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        Log.d(DEBUG_TAG, "onShowPress: " + event.toString());
        if (touchType == 0)
        	note = "touch disabled";
        else
        	note = "show press";
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        Log.d(DEBUG_TAG, "onSingleTapUp: " + event.toString());
        if (touchType == 0)
        	note = "touch disabled";
        else
        	note = "single tap";
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        Log.d(DEBUG_TAG, "onDoubleTap: " + event.toString());
        if (touchType == 0)
        	note = "touch disabled";
        else
        	note = "double tap";
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        Log.d(DEBUG_TAG, "onDoubleTapEvent: " + event.toString());
        if (touchType == 0)
        	note = "touch disabled";
        else
        	note = "double tap event";
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        Log.d(DEBUG_TAG, "onSingleTapConfirmed: " + event.toString());
        if (touchType == 0)
        	note = "touch disabled";
        else
        	note = "single tap";
        return true;
    }  
    
    public void updateLocationResults(String locationString) {
    	//Toast.makeText(this, "Location Updated: " + locationString, Toast.LENGTH_SHORT).show();   
		int delim = locationString.indexOf(',');
		if (delim == -1)
			return;
		String latitude = locationString.substring(0, delim-1);
    	String longitude = locationString.substring(delim+1);
    	if (fetchLocationFirstAttempt) {
    		lon = Double.parseDouble(longitude);
    		lat = Double.parseDouble(latitude);
    		fetchLocationFirstAttempt = false;
    	}
    	else {
    		dz = (Double.parseDouble(longitude) - lon) * METER_PER_LON;
    		dx = (Double.parseDouble(latitude) - lat) * METER_PER_LAT;
    	}
		
		mLocationData.setText("Latitude: " + latitude
				+ ", Longitude: " + longitude + "\ndx: " + String.valueOf(dx)
				+ ", dy: " + String.valueOf(dy) + ", dz: " + String.valueOf(dz));
		
        if (rendererSet) {
        	glSurfaceView.queueEvent(new Runnable() {
        		@Override
        		public void run () {
        			if (locationType == 0)
            			renderer.sensorUpdate(mAzimuth, roll, pitch, 0, 0, 0);
        			else
        				renderer.sensorUpdate(mAzimuth, roll, pitch, dx, dy, dz);
        		}
        	});
        }
    }
    
    TimerTask task = new TimerTask() {
    	String locationString = null;
    	@Override
	    public void run() {
    		String contentString = null;
		    try {
				contentString = getURLContents("http://augmentedrealitymap.appspot.com/json");
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		    
			try {
				JSONObject jObj;
				jObj = new JSONObject(contentString);
				locationString = jObj.optString("lat").toString() + "," + jObj.optString("lon").toString();
				final String loc = locationString;
				if (!loc.equals(prevLoc)) {
					prevLoc = loc;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {						
							updateLocationResults(loc);					
						}
					});
				}
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		    
		}
    	
    	private String getURLContents(String url) throws ClientProtocolException, IOException {
			
			StringBuilder build = new StringBuilder();
			HttpClient client = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(url);
		    HttpResponse response = client.execute(httpGet);
			HttpEntity entity = response.getEntity();
			InputStream content = entity.getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(content));
			String con;
			while ((con = reader.readLine()) != null) {
				build.append(con);
			}
			return build.toString();
		}	
	};
}
