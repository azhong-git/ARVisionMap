package com.example.augmentedreality;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import android.content.Context;
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
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.detection.DetectionBasedTracker;
import com.example.openglbasics.R;

public class ARVIsionActivity extends Activity implements CvCameraViewListener2, SensorEventListener, 
GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener
{
	
	// expandable list view for menu	
	ExpandableListAdapter listAdapter;
	ExpandableListView menuview;
    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;
    static public enum modes {modeWorld, modeVisitor, modeApprentice, modeNavigation, modeCalendar};  
	static public int modeStatus;
    
	// expandable list view for navigation mode
	ExpandableListAdapter deviceListAdapter;
	ExpandableListView deviceview;
    List<String> deviceDataHeader;
    HashMap<String, List<String>> deviceDataChild;
    enum devices {Scanner, Afinia, ProJet, PhotoStudio};
	int deviceStatus;
	
	// sample for visitor mode
	static public enum prototype {TRex, Rex};
	static public int prototypeStatus;
	
    // calendar
    boolean available, scheduled, occupied;
	    
	// OpenGL content view
	private GLSurfaceView glSurfaceView;
	private boolean rendererSet = false;
	private ARVisionRenderer renderer;
		
	// Camera view
	private CameraBridgeViewBase mCameraView;
	// Back camera
	private int mCameraIndex = 0;
	
	// OpenCV objection detection	
	private Mat mRgba;
	private Mat mGray;
	private File mCascadeFile;
	private static final String TAG = "ARVision::Activity";
	
	// OpenGL layout
	FrameLayout view;
	// control panel for visitor mode
	FrameLayout visitorview;
	// control panel for calendar mode
	FrameLayout calendarview;
	// control panel for navigation mode
	FrameLayout navigationview;		
    // Loading text
    TextView mLoadingText;
    
    // check boxes
    CheckBox check_available, check_occupied, check_scheduled;
    
    // button
    Button next_prototype;
    
    SectionsPagerAdapterApprentice mSectionsPagerAdapterApprentice;
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
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        
        mDetector = new GestureDetectorCompat(this,this);
        mScaleDetector = new ScaleGestureDetector(this, this);

        view = (FrameLayout) findViewById(R.id.camera_preview);           
        mLoadingText = (TextView) findViewById(R.id.loading_text);
        
        // get control panel views
        visitorview = (FrameLayout) findViewById(R.id.visitor_control_overlay);
        calendarview = (FrameLayout) findViewById(R.id.calendar_control_overlay);
        navigationview = (FrameLayout) findViewById(R.id.navigation_control_overlay);
        
        // get the listview
        menuview = (ExpandableListView) findViewById(R.id.expandableListView);
        deviceview = (ExpandableListView) findViewById(R.id.deviceExpandableListView);
        // preparing list data
        prepareListData();
        listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild); 
        deviceListAdapter = new ExpandableListAdapter(this, deviceDataHeader, deviceDataChild);
        // setting list adapter
        menuview.setAdapter(listAdapter);        
        modeStatus = modes.modeWorld.ordinal();
        deviceview.setAdapter(deviceListAdapter);
        deviceStatus = devices.Afinia.ordinal();
        
        // initiate prototype status;
        prototypeStatus = prototype.TRex.ordinal();
        next_prototype = (Button) findViewById(R.id.nextPrototypeButton);
                
        check_scheduled = (CheckBox) findViewById(R.id.checkbox_scheduled);
        check_occupied = (CheckBox) findViewById(R.id.checkbox_occupied);
        check_available = (CheckBox) findViewById(R.id.checkbox_available);
        available = false;
	    scheduled = false;
	    occupied = false;
	    calendarview.setVisibility(View.INVISIBLE);
	    navigationview.setVisibility(View.INVISIBLE);
        
        mSectionsPagerAdapterApprentice = new SectionsPagerAdapterApprentice(getFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);
		//mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.setVisibility(View.INVISIBLE);
		
		deviceview.setOnChildClickListener(new OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				// TODO Auto-generated method stub
				deviceStatus = childPosition;
				Toast.makeText(getApplicationContext(), deviceDataHeader.get(groupPosition)+" : "+deviceDataChild.get(deviceDataHeader.get(groupPosition)).get(childPosition), 
                		Toast.LENGTH_SHORT).show();
				
				if (deviceStatus == devices.Scanner.ordinal()) {
					// render direction arrows or signs to 3D scanner
				}
				else if (deviceStatus == devices.Afinia.ordinal()) {
					// render direction arrows or signs to Afinia H-series (the small 3D printer)
				}
				else if (deviceStatus == devices.ProJet.ordinal()) {
					// render direction arrows or signs to ProJet 3000 (the large 3D printer)
				}
				else if (deviceStatus == devices.PhotoStudio.ordinal()) {
					// render direction arrows or signs to Product Photo Studio
				}
				// directions for other devices can be added
				deviceview.collapseGroup(groupPosition);
				return false;
			}
			
		});
		
        menuview.setOnChildClickListener(new OnChildClickListener() {
        	@Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        		
        		modeStatus = childPosition;
                Toast.makeText(getApplicationContext(), listDataHeader.get(groupPosition)+" : "+listDataChild.get(listDataHeader.get(groupPosition)).get(childPosition), 
                		Toast.LENGTH_SHORT).show();
                
                if (modeStatus == modes.modeWorld.ordinal()) {
                	mViewPager.setVisibility(View.INVISIBLE);
        			calendarview.setVisibility(View.INVISIBLE);
        			navigationview.setVisibility(View.INVISIBLE);
        			visitorview.setVisibility(View.INVISIBLE);
                }
                else if (modeStatus == modes.modeVisitor.ordinal()) {
                	mViewPager.setVisibility(View.INVISIBLE);
            		calendarview.setVisibility(View.INVISIBLE);
            		navigationview.setVisibility(View.INVISIBLE);
            		visitorview.setVisibility(View.VISIBLE);
                }                
                else if (modeStatus == modes.modeApprentice.ordinal()) {
                	mViewPager.setAdapter(mSectionsPagerAdapterApprentice);
                	mViewPager.setVisibility(View.VISIBLE);
        			mViewPager.setCurrentItem(0);
            		calendarview.setVisibility(View.INVISIBLE);
            		navigationview.setVisibility(View.INVISIBLE);
            		visitorview.setVisibility(View.INVISIBLE);
                }
                else if (modeStatus == modes.modeCalendar.ordinal()) {
                	mViewPager.setVisibility(View.INVISIBLE);
                	calendarview.setVisibility(View.INVISIBLE);
        			calendarview.setVisibility(View.VISIBLE);
        			calendarview.bringToFront();
        			navigationview.setVisibility(View.INVISIBLE);
        			visitorview.setVisibility(View.INVISIBLE);
                }
                else if (modeStatus == modes.modeNavigation.ordinal()) {
                	if (!rendererSet) {
            			view.addView(glSurfaceView);
            			rendererSet = true;
                	}
                	mViewPager.setVisibility(View.INVISIBLE);
        			calendarview.setVisibility(View.INVISIBLE);
        			navigationview.setVisibility(View.INVISIBLE);
        			navigationview.setVisibility(View.VISIBLE);
        			navigationview.bringToFront();
        			visitorview.setVisibility(View.INVISIBLE);
                }
                menuview.collapseGroup(groupPosition);
                return true;
            }
        });
        
        menuview.setOnGroupExpandListener(new OnGroupExpandListener() {
        	@Override
        	public void onGroupExpand (int groupPosition) {
        		//listDataHeader.set(groupPosition, "Mode Selection");
        	}
        });
        
        // set check function
        check_available.setOnCheckedChangeListener(new OnCheckedChangeListener () {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if (arg1) {
					Toast.makeText(calendarview.getContext(), "Available Checked", Toast.LENGTH_SHORT).show();
					available = true;
				}
				else {
					Toast.makeText(calendarview.getContext(), "Available Unchecked", Toast.LENGTH_SHORT).show();
					available = false;
				}
				//updateDeviceStatus();
			}
		});
        
        check_occupied.setOnCheckedChangeListener(new OnCheckedChangeListener () {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if (arg1) {
					Toast.makeText(calendarview.getContext(), "Occupied Checked", Toast.LENGTH_SHORT).show();
					occupied = true;
				}
				else {
					Toast.makeText(calendarview.getContext(), "Occupied Unchecked", Toast.LENGTH_SHORT).show();
					occupied = false;
				}
				//updateDeviceStatus();
			}
		});
        
        check_scheduled.setOnCheckedChangeListener(new OnCheckedChangeListener () {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if (arg1) {
					Toast.makeText(calendarview.getContext(), "Scheduled Checked", Toast.LENGTH_SHORT).show();
					scheduled = true;
				}
				else {
					Toast.makeText(calendarview.getContext(), "Scheduled Unchecked", Toast.LENGTH_SHORT).show();
					scheduled = false;
				}				
			}
		});
        
        ////////
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
    	// Toast.makeText(this, "Location Fetched: " + locationString, Toast.LENGTH_SHORT).show();
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
	
	
	public class SectionsPagerAdapterApprentice extends FragmentStatePagerAdapter {

		public SectionsPagerAdapterApprentice(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			return PlaceholderFragmentApprentice.newInstance(position + 1);
		}

		@Override
		public int getCount() {
			return 3;
		}
	}

	public static class PlaceholderFragmentApprentice extends Fragment {
		private static final String ARG_SECTION_NUMBER = "section_number";

		public static PlaceholderFragmentApprentice newInstance(int sectionNumber) {
			PlaceholderFragmentApprentice fragment = new PlaceholderFragmentApprentice();
			Bundle args = new Bundle();
			args.putInt(ARG_SECTION_NUMBER, sectionNumber);
			fragment.setArguments(args);
			return fragment;
		}

		public PlaceholderFragmentApprentice() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			int sectionNum = getArguments().getInt(ARG_SECTION_NUMBER);
			TextView textView = (TextView) rootView.findViewById(R.id.section_label);
			textView.setText(Integer.toString(sectionNum));
			
			ImageView imgView = (ImageView) rootView.findViewById(R.id.image_view);
			
			if (sectionNum == 1)
				imgView.setImageResource(R.drawable.step1);
			else if (sectionNum == 2)
				imgView.setImageResource(R.drawable.step2);
			else if (sectionNum == 3)
				imgView.setImageResource(R.drawable.step3);
			return rootView;
		}
	}	
	
	private void prepareListData() {
        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<String>>();
        deviceDataHeader = new ArrayList<String>();
        deviceDataChild = new HashMap<String, List<String>>();
        
        // Adding child data
        listDataHeader.add("Mode Selection");
        deviceDataHeader.add("Device Selection");
 
        // Adding child data
        List<String> modegroup = new ArrayList<String>();
        modegroup.add("World");
        modegroup.add("Visitor");
        modegroup.add("Apprentice");
        modegroup.add("Navigation");
        modegroup.add("Calendar");
        List<String> devicegroup = new ArrayList<String>();
        devicegroup.add("3D Scanner");
        devicegroup.add("Afinia H-series");
        devicegroup.add("ProJet 3000");
        devicegroup.add("Product Photo Studio");
 
        listDataChild.put(listDataHeader.get(0), modegroup); // Header, Child data
        deviceDataChild.put(deviceDataHeader.get(0), devicegroup);
    }

	// button click event
	public void myButtonClickHandler(View view) {
        switch (view.getId()) {
        case R.id.nextPrototypeButton:
        	if (prototypeStatus == prototype.TRex.ordinal()) {
        		prototypeStatus = prototype.Rex.ordinal();
        	}
        	else {
        		prototypeStatus = prototype.TRex.ordinal();
        	}
        	Toast.makeText(this, "Prototype Button Clicked", Toast.LENGTH_SHORT).show();
            break;
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
