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
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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

public class ARVIsionActivity extends Activity implements CvCameraViewListener2, SensorEventListener {
	
	// expandable list view for menu	
	ExpandableListAdapter listAdapter;
	ExpandableListView menuview;
    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;
    enum modes {modeWorld, modeVisitor, modeApprentice, modeNavigation, modeCalendar};  
	int modeStatus;
    
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
	// control panel for calendar mode
	FrameLayout calendarview;
    // Loading text
    TextView mLoadingText;
    
    // check boxes
    CheckBox check_available, check_occupied, check_scheduled;
    
    SectionsPagerAdapter mSectionsPagerAdapter;
	ViewPager mViewPager;
	
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
    
    private boolean mFailed;
    
	private final float ALPHA = 0.15f;
	
	// home center
	double lat = 0.0;//= 37.875133;
	double lon = 0.0;//= -122.2595580;
	double dx = 0.0;
	double dy = 0.0;
	double dz = 0.0;
	boolean fetchLocationFirstAttempt = true;
	
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

        view = (FrameLayout) findViewById(R.id.camera_preview);           
        mLoadingText = (TextView) findViewById(R.id.loading_text);
        
        /////////
        calendarview = (FrameLayout) findViewById(R.id.calendar_control_overlay);
        
        // code for setting menu items - CY: start
        
        // get the listview
        menuview = (ExpandableListView) findViewById(R.id.expandableListView);
        // preparing list data
        prepareListData();
        listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild); 
        // setting list adapter
        menuview.setAdapter(listAdapter);        
        modeStatus = modes.modeWorld.ordinal();
                
        check_scheduled = (CheckBox) findViewById(R.id.checkbox_scheduled);
        check_occupied = (CheckBox) findViewById(R.id.checkbox_occupied);
        check_available = (CheckBox) findViewById(R.id.checkbox_available);
        available = false;
	    scheduled = false;
	    occupied = false;
	    calendarview.setVisibility(View.INVISIBLE);
        
        menuview.setOnChildClickListener(new OnChildClickListener() {
        	@Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        		
        		modeStatus = childPosition;
                Toast.makeText(getApplicationContext(), listDataHeader.get(groupPosition)+" : "+listDataChild.get(listDataHeader.get(groupPosition)).get(childPosition), 
                		Toast.LENGTH_SHORT).show();
                
                if (modeStatus == modes.modeWorld.ordinal()) {
                	if (!rendererSet) {
            			view.addView(glSurfaceView);
            			rendererSet = true;
                	}
                	mViewPager.setVisibility(View.INVISIBLE);
        			calendarview.setVisibility(View.INVISIBLE);
                }
                else if (modeStatus == modes.modeVisitor.ordinal()) {
                	if (rendererSet) {
            			view.removeView(glSurfaceView);
            			rendererSet = false;
                	}
                	mViewPager.setVisibility(View.VISIBLE);
        			mViewPager.setCurrentItem(0);
            		calendarview.setVisibility(View.INVISIBLE);
                }                
                else if (modeStatus == modes.modeApprentice.ordinal()) {
                	if (rendererSet) {
            			view.removeView(glSurfaceView);
            			rendererSet = false;            			
                	}  
                	mViewPager.setVisibility(View.VISIBLE);
        			mViewPager.setCurrentItem(0);
            		calendarview.setVisibility(View.INVISIBLE);
                }
                else if (modeStatus == modes.modeCalendar.ordinal()) {
                	if (!rendererSet) {
            			view.addView(glSurfaceView);
            			rendererSet = true;
                	}
                	mViewPager.setVisibility(View.INVISIBLE);
                	calendarview.setVisibility(View.INVISIBLE);
        			calendarview.setVisibility(View.VISIBLE);
        			calendarview.bringToFront();
                }
                else if (modeStatus == modes.modeNavigation.ordinal()) {
                	if (!rendererSet) {
            			view.addView(glSurfaceView);
            			rendererSet = true;
                	}
                	mViewPager.setVisibility(View.INVISIBLE);
        			calendarview.setVisibility(View.INVISIBLE);
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
        timer.schedule(task, 0l, 10000l);   
        
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.setVisibility(View.INVISIBLE);
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
    		dz = (Double.parseDouble(longitude) - lon) * 88070;
    		dx = (Double.parseDouble(latitude) - lat) * 110992;
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
				runOnUiThread(new Runnable() {
					@Override
					public void run() {						
						updateLocationResults(loc);					
					}
				});
				
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
	
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			return PlaceholderFragment.newInstance(position + 1);
		}

		@Override
		public int getCount() {
			return 4;
		}
	}

	public static class PlaceholderFragment extends Fragment {
		private static final String ARG_SECTION_NUMBER = "section_number";

		public static PlaceholderFragment newInstance(int sectionNumber) {
			PlaceholderFragment fragment = new PlaceholderFragment();
			Bundle args = new Bundle();
			args.putInt(ARG_SECTION_NUMBER, sectionNumber);
			fragment.setArguments(args);
			return fragment;
		}

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			int sectionNum = getArguments().getInt(ARG_SECTION_NUMBER);
			TextView textView = (TextView) rootView.findViewById(R.id.section_label);
			textView.setText(Integer.toString(sectionNum));
			
			ImageView imgView = (ImageView) rootView.findViewById(R.id.image_view);
			
			if (sectionNum == 1)
				imgView.setImageResource(R.drawable.product1);
			else if (sectionNum == 2)
				imgView.setImageResource(R.drawable.product2);
			else if (sectionNum == 3)
				imgView.setImageResource(R.drawable.product3);
			else if (sectionNum == 4)
				imgView.setImageResource(R.drawable.product4);
			return rootView;
		}
	}
	
	private void prepareListData() {
        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<String>>();
 
        // Adding child data
        listDataHeader.add("Mode Selection");
 
        // Adding child data
        List<String> modegroup = new ArrayList<String>();
        modegroup.add("World");
        modegroup.add("Visitor");
        modegroup.add("Apprentice");
        modegroup.add("Navigation");
        modegroup.add("Calendar");
 
        listDataChild.put(listDataHeader.get(0), modegroup); // Header, Child data
    }
}
