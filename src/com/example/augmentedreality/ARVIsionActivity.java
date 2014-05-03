package com.example.augmentedreality;

import java.io.BufferedReader;
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
import org.opencv.core.Mat;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.openglbasics.R;

public class ARVIsionActivity extends Activity implements CvCameraViewListener2, SensorEventListener, 
GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener
{   
	Menu actionBarMenu;
	
    static public enum modes {modeWorld, modeVisitor, modeApprentice, modeNavigation, modeCalendar};  
	static public int modeStatus;
    
    static public enum devices {Afinia, ProJet, PhotoStudio, VLSLaserCutter, PowerElectronics}; 
    static final String [] listOfDevices = {"Afinia H-series", "ProJet 3000", "Photo Studio", "VLS Laser Cutter", "Power Electronics"}; 
	static public int currentDevice;
	
	// sample for visitor mode
	static public enum prototypes {TRex, Prototype1, Prototype2};
	static public int currentPrototypeAfinia, currentPrototypePJ,
			currentPrototypePE, currentPrototypePS, currentPrototypeLC;
	
    // calendar state variables
    static public boolean available, scheduled, occupied;
	    
	// OpenGL content view
	private GLSurfaceView glSurfaceView;
	private boolean rendererSet = false;
	private ARVisionRenderer renderer;
		
	// Camera view
	private CameraBridgeViewBase mCameraView;
	// Back camera
	private int mCameraIndex = 0;
	
	// OpenCV objection detection	
	private Mat mRgba, mGray;
	
	// OpenGL layout
	FrameLayout view;
    // Loading text
    TextView mLoadingText;
    
    ApprenticeModeGalleryAdapter mApprenticeModeGalleryAdapter;
	ViewPager mViewPager;
	
	private GestureDetectorCompat mDetector; 
	private ScaleGestureDetector mScaleDetector;
	
	// sensors
    private SensorManager mSensMan;
    static public float mAzimuth, roll, pitch;

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
	double lat = 37.87474;
	double lon = -122.25850;
	public static int METER_PER_LAT = 110994;
	public static int METER_PER_LON = 87980;
	double dx = 0.0;
	double dy = 0.0;
	double dz = 0.0;
	static float rotateX = -90;
	static float rotateZ = 0;
	static float zoom = 1;

	// if true: initial map loaded relatively, if false use lat and lon as defined
	boolean fetchLocationFirstAttempt = false;
	private String prevLoc = "";
	
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
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
	public boolean onCreateOptionsMenu(Menu menu) {
    	actionBarMenu = menu;
		getMenuInflater().inflate(R.menu.action_menu, menu);
		 	
		Spinner modesSpinner = (Spinner) menu.findItem(R.id.action_modes_spinner).getActionView();
		SpinnerAdapter modesSpinnerAdapter = ArrayAdapter.createFromResource(getActionBar().getThemedContext(),
				R.array.modes_list, android.R.layout.simple_spinner_dropdown_item);
		modesSpinner.setAdapter(modesSpinnerAdapter);		
		modesSpinner.setOnItemSelectedListener(new ModesHandler());
		
		Spinner devicesSpinner = (Spinner) menu.findItem(R.id.action_devices_spinner).getActionView();
		SpinnerAdapter devicesSpinnerAdapter = ArrayAdapter.createFromResource(getActionBar().getThemedContext(),
				R.array.devices_list, android.R.layout.simple_spinner_dropdown_item);
		devicesSpinner.setAdapter(devicesSpinnerAdapter);
		devicesSpinner.setOnItemSelectedListener(new DevicesHandler());	        
        
		return true;
	}
	  
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	boolean isChecked;
	    switch (item.getItemId()) {
	        case R.id.action_next:
	        	currentPrototypeAfinia++;
	        	if (currentPrototypeAfinia == prototypes.values().length)
	        		currentPrototypeAfinia = 0;
	            break;
	            
	        case R.id.action_prev:
	        	currentPrototypeAfinia--;
	        	if (currentPrototypeAfinia == -1)
	        		currentPrototypeAfinia = prototypes.values().length - 1;
	            break;
	            
	        case R.id.action_cb_occupied:
	        	isChecked = item.isChecked();
	        	item.setChecked(!isChecked);
	        	occupied = !isChecked;
	        	if (occupied)
	        		actionBarMenu.findItem(R.id.action_cb_oc_icon).setIcon(R.drawable.square_green);
	        	else
	        		actionBarMenu.findItem(R.id.action_cb_oc_icon).setIcon(R.drawable.square_white);
	            break;
	            
	        case R.id.action_cb_available:
	        	isChecked = item.isChecked();
	        	item.setChecked(!isChecked);
	        	available = !isChecked;
	        	if (available)
	        		actionBarMenu.findItem(R.id.action_cb_av_icon).setIcon(R.drawable.square_green);
	        	else
	        		actionBarMenu.findItem(R.id.action_cb_av_icon).setIcon(R.drawable.square_white);
	            break;
	            
	        case R.id.action_cb_scheduled:
	        	isChecked = item.isChecked();
	        	item.setChecked(!isChecked);
	        	scheduled = !isChecked;
	        	if (scheduled)
	        		actionBarMenu.findItem(R.id.action_cb_sc_icon).setIcon(R.drawable.square_green);
	        	else
	        		actionBarMenu.findItem(R.id.action_cb_sc_icon).setIcon(R.drawable.square_white);
	            break;
	    }
	    return true;	    
	}
    
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().addFlags(
        		WindowManager.LayoutParams.FLAG_FULLSCREEN |
        		WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        
        mDetector = new GestureDetectorCompat(this,this);
        mScaleDetector = new ScaleGestureDetector(this, this);

        view = (FrameLayout) findViewById(R.id.camera_preview);           
        mLoadingText = (TextView) findViewById(R.id.loading_text);
        
        modeStatus = modes.modeWorld.ordinal();
		currentDevice = devices.Afinia.ordinal();
        currentPrototypeAfinia = prototypes.TRex.ordinal();
        
        available = false;
	    scheduled = false;
	    occupied = false;
        
        mApprenticeModeGalleryAdapter = new ApprenticeModeGalleryAdapter(getFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setVisibility(View.INVISIBLE);
        
        initCamera();

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
        timer = new Timer();
        timer.schedule(task, 0l, timerInterval);   
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
        mCameraView.setMinimumWidth(1920);
    }
    
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	mRgba = inputFrame.rgba();
    	mGray = inputFrame.gray();
    	    	
        if (ARVisionRenderer.GLStatus == ARVisionRenderer.GraphicsStatus.Loading) {        	
        	runOnUiThread(new Runnable() {
				@Override
				public void run() {						
					mLoadingText.setVisibility(View.VISIBLE);
				}
			});
        }
        else {
        	runOnUiThread(new Runnable() {
				@Override
				public void run() {						
					mLoadingText.setVisibility(View.INVISIBLE);	
				}
			});
        }
        return mRgba;
    }
        
    private void initCamera() {
    	mCameraView = new JavaCameraView(this, mCameraIndex);
    	mCameraView.setCvCameraViewListener(this);
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
        	SensorManager.remapCoordinateSystem(mRotationM, SensorManager.AXIS_X,
    					SensorManager.AXIS_Z, mRemapedRotationM);
    		SensorManager.getOrientation(mRemapedRotationM, mOrientation);
            onSuccess();
            if (rendererSet) {
            	glSurfaceView.queueEvent(new Runnable() {
            		@Override
            		public void run () {
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
            Toast.makeText(this, "Failed to retrive rotation Matrix", Toast.LENGTH_LONG).show();
            //sensorAvailable = false;
        }
	}	
    
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    private void initGl() {
        renderer = new ARVisionRenderer(this);
        glSurfaceView = new GLSurfaceView(this);
    	glSurfaceView.setEGLContextClientVersion(2);
    	glSurfaceView.setZOrderMediaOverlay(true);
    	glSurfaceView.setEGLConfigChooser(8,8,8,8,16,0);
    	glSurfaceView.setRenderer(renderer);
    	glSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
    	rendererSet = true;
        view.addView(glSurfaceView);
    }
    
    public void updateLocationResults(String locationString) {
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
		
        if (rendererSet) {
        	glSurfaceView.queueEvent(new Runnable() {
        		@Override
        		public void run () {
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
	
	private class ModesHandler implements AdapterView.OnItemSelectedListener{

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			modeStatus = position;
			setViewsInvisible();
			
            if (modeStatus == modes.modeWorld.ordinal()) {
    			mCameraView.setVisibility(View.VISIBLE);
            }
            else if (modeStatus == modes.modeVisitor.ordinal()) {
            	actionBarMenu.findItem(R.id.action_devices_spinner).setVisible(true);
	    		currentPrototypeAfinia = prototypes.TRex.ordinal();
    	        currentPrototypePJ = currentPrototypePE = currentPrototypePS = currentPrototypeLC = 0;
	    		if (currentDevice == devices.Afinia.ordinal()) {
	    			actionBarMenu.findItem(R.id.action_next).setVisible(true);
		    		actionBarMenu.findItem(R.id.action_prev).setVisible(true);
	    		}
	    		else {
	    			mViewPager.setAdapter(mApprenticeModeGalleryAdapter);
	            	mViewPager.setVisibility(View.VISIBLE);
	    			mViewPager.setCurrentItem(0);
	    			mViewPager.bringToFront();
	    		}
            }                
            else if (modeStatus == modes.modeApprentice.ordinal()) {
            	actionBarMenu.findItem(R.id.action_devices_spinner).setVisible(true);
        		mViewPager.setAdapter(mApprenticeModeGalleryAdapter);
            	mViewPager.setVisibility(View.VISIBLE);
    			mViewPager.setCurrentItem(0);
            }
            else if (modeStatus == modes.modeNavigation.ordinal()) {
            	actionBarMenu.findItem(R.id.action_devices_spinner).setVisible(true);
        		mCameraView.setVisibility(View.VISIBLE); 
            }
            else if (modeStatus == modes.modeCalendar.ordinal()) {
    			mCameraView.setVisibility(View.VISIBLE); 
    			actionBarMenu.findItem(R.id.action_cb_av_icon).setVisible(true);
    			actionBarMenu.findItem(R.id.action_cb_available).setVisible(true);
    			actionBarMenu.findItem(R.id.action_cb_oc_icon).setVisible(true);
    			actionBarMenu.findItem(R.id.action_cb_occupied).setVisible(true);
    			actionBarMenu.findItem(R.id.action_cb_sc_icon).setVisible(true);
    			actionBarMenu.findItem(R.id.action_cb_scheduled).setVisible(true);
            }
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			// TODO Auto-generated method stub
			
		}				
	}
	
	public void setViewsInvisible() {
		mViewPager.setVisibility(View.INVISIBLE);
		mCameraView.setVisibility(View.INVISIBLE);
		actionBarMenu.findItem(R.id.action_devices_spinner).setVisible(false);
		actionBarMenu.findItem(R.id.action_next).setVisible(false);
		actionBarMenu.findItem(R.id.action_prev).setVisible(false);
		actionBarMenu.findItem(R.id.action_cb_av_icon).setVisible(false);
		actionBarMenu.findItem(R.id.action_cb_available).setVisible(false);
		actionBarMenu.findItem(R.id.action_cb_oc_icon).setVisible(false);
		actionBarMenu.findItem(R.id.action_cb_occupied).setVisible(false);
		actionBarMenu.findItem(R.id.action_cb_sc_icon).setVisible(false);
		actionBarMenu.findItem(R.id.action_cb_scheduled).setVisible(false);
	}
	
	private class DevicesHandler implements AdapterView.OnItemSelectedListener{

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			currentDevice = position;
			
			if (currentDevice == devices.Afinia.ordinal() 
					&& modeStatus == modes.modeVisitor.ordinal()) {
				actionBarMenu.findItem(R.id.action_next).setVisible(true);
	    		actionBarMenu.findItem(R.id.action_prev).setVisible(true);
	    		mViewPager.setVisibility(View.INVISIBLE);
			}
			else if (modeStatus == modes.modeApprentice.ordinal()
					|| modeStatus == modes.modeVisitor.ordinal()) {
				mApprenticeModeGalleryAdapter.notifyDataSetChanged();
    			actionBarMenu.findItem(R.id.action_next).setVisible(false);
	    		actionBarMenu.findItem(R.id.action_prev).setVisible(false);
    			mViewPager.setAdapter(mApprenticeModeGalleryAdapter);
            	mViewPager.setVisibility(View.VISIBLE);
    			mViewPager.setCurrentItem(0);
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			// TODO Auto-generated method stub
			
		}				
	}
	
	public class ApprenticeModeGalleryAdapter extends FragmentStatePagerAdapter {
		
		public int numStepsAfiniaApprentice = 3,
				numStepsLCApprentice = 4,
				numStepsPJApprentice = 1,
				numStepsPSApprentice = 1,
				numStepsPEApprentice = 3;
		
		public int numStepsLCVisitor = 3,
				numStepsPJVisitor = 3,
				numStepsPSVisitor = 1,
				numStepsPEVisitor = 1;
		
		public ApprenticeModeGalleryAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			return ApprenticeModeGalleryFragment.newInstance(position + 1);
		}

		@Override
		public int getItemPosition(Object object) {
		    return POSITION_NONE;
		}
		
		@Override
		public int getCount() {
			int count = 0;
			if (modeStatus == modes.modeApprentice.ordinal()) {
				if (currentDevice == devices.Afinia.ordinal())
					count = numStepsAfiniaApprentice;
				else if (currentDevice == devices.VLSLaserCutter.ordinal())
					count = numStepsLCApprentice;
				else if (currentDevice == devices.PhotoStudio.ordinal())
					count = numStepsPSApprentice;
				else if (currentDevice == devices.ProJet.ordinal())
					count = numStepsPJApprentice;
				else if (currentDevice == devices.PowerElectronics.ordinal())
					count = numStepsPEApprentice;
			}
			else if (modeStatus == modes.modeVisitor.ordinal()) {
				if (currentDevice == devices.VLSLaserCutter.ordinal())
					count = numStepsLCVisitor;
				else if (currentDevice == devices.PhotoStudio.ordinal())
					count = numStepsPSVisitor;
				else if (currentDevice == devices.ProJet.ordinal())
					count = numStepsPJVisitor;
				else if (currentDevice == devices.PowerElectronics.ordinal())
					count = numStepsPEVisitor;
			}
			return count;
		}
	}

	public static class ApprenticeModeGalleryFragment extends Fragment {
		private static final String ARG_SECTION_NUMBER = "section_number";

		public static ApprenticeModeGalleryFragment newInstance(int sectionNumber) {
			ApprenticeModeGalleryFragment fragment = new ApprenticeModeGalleryFragment();
			Bundle args = new Bundle();
			args.putInt(ARG_SECTION_NUMBER, sectionNumber);
			fragment.setArguments(args);
			return fragment;
		}

		public ApprenticeModeGalleryFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			int sectionNum = getArguments().getInt(ARG_SECTION_NUMBER);
			TextView textView = (TextView) rootView.findViewById(R.id.section_label);
			textView.setText(Integer.toString(sectionNum));
			ImageView imgView = (ImageView) rootView.findViewById(R.id.image_view);

			if (modeStatus == modes.modeApprentice.ordinal()) {
				if (currentDevice == devices.Afinia.ordinal()) {
					if (sectionNum == 1)
						imgView.setImageResource(R.drawable.af_step1);
					else if (sectionNum == 2)
						imgView.setImageResource(R.drawable.af_step2);
					else if (sectionNum == 3)
						imgView.setImageResource(R.drawable.af_step3);
				}
				else if (currentDevice == devices.VLSLaserCutter.ordinal()) {
					if (sectionNum == 1)
						imgView.setImageResource(R.drawable.lc_step1);
					else if (sectionNum == 2)
						imgView.setImageResource(R.drawable.lc_step2);
					else if (sectionNum == 3)
						imgView.setImageResource(R.drawable.lc_step3);
					else if (sectionNum == 4)
						imgView.setImageResource(R.drawable.lc_step4);
				}
				else if (currentDevice == devices.ProJet.ordinal()) {
					if (sectionNum == 1)
						imgView.setImageResource(R.drawable.pj_step1);
				}
				else if (currentDevice == devices.PhotoStudio.ordinal()) {
					if (sectionNum == 1)
						imgView.setImageResource(R.drawable.ps_step1);
				}
				else if (currentDevice == devices.PowerElectronics.ordinal()) {
					if (sectionNum == 1)
						imgView.setImageResource(R.drawable.pe_step1);
					else if (sectionNum == 2)
						imgView.setImageResource(R.drawable.pe_step2);
					else if (sectionNum == 3)
						imgView.setImageResource(R.drawable.pe_step3);
				}
			}
			else if (modeStatus == modes.modeVisitor.ordinal()) {
				if (currentDevice == devices.VLSLaserCutter.ordinal()) {
					if (sectionNum == 1)
						imgView.setImageResource(R.drawable.lc_p1);
					else if (sectionNum == 2)
						imgView.setImageResource(R.drawable.lc_p3);
					else if (sectionNum == 3)
						imgView.setImageResource(R.drawable.lc_p2);
				}
				else if (currentDevice == devices.ProJet.ordinal()) {
					if (sectionNum == 1)
						imgView.setImageResource(R.drawable.pj_p1);
					else if (sectionNum == 2)
						imgView.setImageResource(R.drawable.pj_p2);
					else if (sectionNum == 3)
						imgView.setImageResource(R.drawable.pj_p3);
				}
				else if (currentDevice == devices.PhotoStudio.ordinal()) {
					if (sectionNum == 1)
						imgView.setImageResource(R.drawable.ps_step1);
				}
				else if (currentDevice == devices.PowerElectronics.ordinal()) {
					if (sectionNum == 1)
						imgView.setImageResource(R.drawable.pe_p1);
				}
			}
			return rootView;
		}
	}
	
	@Override 
    public boolean onTouchEvent(MotionEvent event){ 
		boolean res = this.mScaleDetector.onTouchEvent(event);
		boolean isScaling = res = mScaleDetector.isInProgress();
		if (!isScaling)
			res = this.mDetector.onTouchEvent(event);
        return res? res : super.onTouchEvent(event);
    }
	 
	@Override
	public boolean onDown(MotionEvent arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2,
			float arg3) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onLongPress(MotionEvent arg0) {
		// TODO Auto-generated method stub
		Log.d("DEBUG", "onLongPress: " + arg0.toString());
		float x = arg0.getX();
		float half = (x-600)/600*(float)(Math.tan(Math.toRadians(35)) * ARVisionRenderer.near);
		
		float angle = (float) (mAzimuth + Math.toDegrees(Math.atan(half/ARVisionRenderer.near)));
		int deviceNo = ARVisionRenderer.getNearestDevice((float)dx, (float)dy, (float)dz, angle);

		Log.d("DEBUG", "onLongPress: Angle: " + angle);
		Log.d("DEBUG", "onLongPress: Device: " + deviceNo);
		if (deviceNo >= 0)
			Toast.makeText(getApplicationContext(), listOfDevices[deviceNo], Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, final float distanceX,
            final float distanceY) {
		// TODO Auto-generated method stub
		Log.d("DEBUG", "onScroll: " + e1.toString()+e2.toString());
		if (Math.abs(distanceX) > Math.abs(distanceY))
			rotateZ = (rotateZ - distanceX) % 360;
		else
			rotateX = (rotateX - distanceY) % 360;
		return true;
	}

	@Override
	public void onShowPress(MotionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		// TODO Auto-generated method stub
		zoom = 1/detector.getScaleFactor();
		return false;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {
		// TODO Auto-generated method stub
		
	}
}
