package com.example.augmentedreality;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.example.detection.DetectionBasedTracker;
import com.example.openglbasics.R;

public class ARVIsionActivity extends Activity implements CvCameraViewListener2, SensorEventListener, LocationListener, GestureDetector.OnGestureListener,
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
	private static final Scalar RECT_COLOR = new Scalar (0, 0, 255, 100);
	private static final Scalar FONT_COLOR = new Scalar (0, 76, 253, 10);
	private static final Scalar MENU_COLOR = new Scalar (255, 0, 125, 100);
	
	private Mat mRgba;
	private Mat mGray;
	private File mCascadeFile;
	//private CascadeClassifier mDetector;
	private DetectionBasedTracker mNativeDetector;
	private static final String TAG = "ARVision::Activity";
    private float                  mRelativeFaceSize   = 0.5f;
    private int                    mAbsoluteFaceSize   = 0;
    private int x1, x2, x3, midx, xx;
    private int y1, y2, y3, midy, yy;
	
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
    private boolean mFailed;
    
	private final float ALPHA = 0.15f;

	private LocationManager locationManager;
	private String provider;
	private boolean locationMechanismSet = false;
	
	// home center
	double lat ;//= 37.875133;
	double lon ;//= -122.2595580;
	double dx;
	double dy;
	double dz;
	private int counter = 0;
	private boolean locationCali = false;

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
                	 
                		mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);
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
        
        // Get the location manager
        try {
	        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	        locationMechanismSet = true;
        } catch (Exception e) {
        	System.out.println("Location service could not be initialized: " + e);
        }
	        
        if (locationMechanismSet) {
	        Criteria criteria = new Criteria();        
		    provider = locationManager.getBestProvider(criteria, false);
		    Location location = locationManager.getLastKnownLocation(provider);

	        if (location != null) {
	          System.out.println("Provider " + provider + " has been selected.");
			  lon = location.getLongitude();
			  lat = location.getLatitude();
	          onLocationChanged(location);
	        } else {
	        	mLocationData.setText("Location not available.");
	        }
        }

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
    	
    	if (locationMechanismSet)
    		locationManager.removeUpdates(this);
    }

    @Override
    protected void onResume() {
    	super.onResume();
    	if (rendererSet) {
    		glSurfaceView.onResume();
    	}
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
        
    	if (locationMechanismSet)
    		locationManager.requestLocationUpdates(provider, 0, 0, this);
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
        yy = 50;
        x1 = 100;
        y1 = 50;
        x2 = 300;
        y2 = 150;
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
    	// Used for detection
    	/*
        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }
        
        mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        
        MatOfRect objects = new MatOfRect();

        if (mNativeDetector != null) {
        	mNativeDetector.detect(mGray, objects);
        }
        else {
        	Log.e(TAG, "Detection method is not available!");
        }
        Rect [] objectsArray = objects.toArray();
        for (int i = 0; i < objectsArray.length; i++) {
            Core.rectangle(mRgba, objectsArray[i].tl(), objectsArray[i].br(), RECT_COLOR, 3);
            Core.putText(mRgba, "Surf bus here?", objectsArray[i].tl(), 0, 1.25, FONT_COLOR, 2);
        }
    	 */
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
                /*
                 * NOTE: The data must be copied off the event.values
                 * as the system is reusing that array in all SensorEvents.
                 * Simply assigning:
                 * mGravs = event.values won't work.
                 *
                 * I use a member array in an attempt to reduce garbage production.
                 */
        		mGravs = lowPass(event.values.clone(), mGravs);
                break;
        	case Sensor.TYPE_MAGNETIC_FIELD:
                mGeoMags = lowPass(event.values.clone(), mGeoMags);
        	default:
        		return;
        }

        if (SensorManager.getRotationMatrix(mRotationM, null, mGravs,mGeoMags)){
//                Rotate to the camera's line of view (Y axis along the camera's axis)
//                TODO: Find how to use android.opengl.Matrix to rotate to an arbitrary coordinate system.
        		Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
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
        //        Convert the azimuth to degrees in 0.5 degree resolution.
        mAzimuth = (float) Math.round((Math.toDegrees(mOrientation[0])) *2)/2;
        //        Adjust the range: 0 < range <= 360 (from: -180 < range <= 180).
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
	public void onLocationChanged(Location location) {
		if (!locationCali) {
			counter++;
			if (counter > 50) {
				lat = (location.getLatitude());
				lon = (location.getLongitude());
				locationCali = true;
			}
		}
		double latitude = (location.getLatitude());
	    double longitude = (location.getLongitude());
		//mLocationData.setText("Latitude: " + String.valueOf(latitude)
	    //	+ ", Longitude: " + String.valueOf(longitude));
		dz = (longitude - lon) * 88070;
		dx = (latitude - lat) * 110992;
		
		mLocationData.setText("Ready: " + locationCali + ", Lati: " + String.valueOf(latitude)
				+ ", Long: " + String.valueOf(longitude) + "\n" + "dx: " + String.valueOf(dx)
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

	@Override
	public void onProviderDisabled(String provider) {
		Toast.makeText(this, "Disabled provider " + provider,
		        Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onProviderEnabled(String provider) {
		Toast.makeText(this, "Enabled new provider " + provider,
		        Toast.LENGTH_SHORT).show();		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
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

    
}
