package com.example.augmentedreality;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;

import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_ONE;
import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glViewport;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.setLookAtM;
import static android.opengl.Matrix.translateM;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLSurfaceView.Renderer;

import com.example.objects.Arrow;
import com.example.objects.Caption;
import com.example.objects.Puck;
import com.example.objects.Square;
import com.example.objects.RawObject;
import com.example.openglbasics.R;
import com.example.util.ColorShaderProgram;
import com.example.util.MatrixHelper;
import com.example.util.ObjectShaderProgram;
import com.example.util.TextureHelper;
import com.example.util.TextureShaderProgram;
import com.example.util.TransparentTextureShaderProgram;

public class ARVisionRenderer implements Renderer{
	
	private final Context context;
	private final int MAXNUM_TEXTURES = 10;
	
	private final float [] projectionMatrix = new float[16];
	private final float [] viewMatrix = new float [16];
	
	// model, view, final matrices for each object
	private final float [][] modelMatrix = new float[MAXNUM_TEXTURES][];
	private final float [][] modelViewMatrix = new float[MAXNUM_TEXTURES][];
	private final float [][] finalMatrix = new float[MAXNUM_TEXTURES][];
	
	// helper matrices rotation and translation
	private final float [][] rotationMatrix = new float[MAXNUM_TEXTURES][];
	private final float [][] translationMatrix = new float[MAXNUM_TEXTURES][];
	
	private final float [] compassModelMatrix = new float [16];
	private final float [] compassRotationMatrix = new float [16];
	private final float [] compassTranslationMatrix = new float [16];
	private final float [] compassViewMatrix = new float [16];
	private final float [] compassModelViewMatrix = new float [16];
	private final float [] compassFinalMatrix = new float [16];

	// projection matrix parameters
	private final float far = 10f;
	private final float near = 0.2f;
	private final float ztranslate = 2.5f;
	private final float busz = 1.0f;
	private final float zpuck = 7f;
	private final float zcompass = 1f;
	
	private float cameradx = 0;
	private float camerady = 0;
	private float cameradz = 0;
	
	private float x = 0;
	private float y = 0;
	private float z = 0;
	
	float [] forward = new float [3];
	float [] up_portrait = new float [3];
	float [] up_landscape = new float [3];
	
	private enum Mode {
		MoveObject, MoveCamera;
	}
	
	private Mode mode = Mode.MoveCamera;

	// number of textures
	private int numCaptionTextures = 2;
	private int numRawObjectTextures = 3;
	private int numObjects = numCaptionTextures + numRawObjectTextures + 1;
	
	// caption object array
	private Caption [] caption = new Caption [numCaptionTextures];
	private RawObject [] object = new RawObject [numRawObjectTextures];
	
	// texture Ids
	private int [] textures = new int [numCaptionTextures + numRawObjectTextures];
	
	private Square compass = new Square();
	private int compassTexture;
	
	private Arrow puck = new Arrow(1, 2, (float)0.5 ,1 ,32);
	
	//private Block block;
	
	private TextureShaderProgram textureProgram;
	private TransparentTextureShaderProgram transparentProgram;
	private ColorShaderProgram colorProgram;
	private ObjectShaderProgram objectProgram;

	
	public ARVisionRenderer(Context context) {
		this.context = context;
	}
	
	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
		mode = Mode.MoveCamera;
		
		for (int i = 0; i < numObjects; i++) {
			modelMatrix[i] = new float [16];
			modelViewMatrix[i] = new float [16];
			finalMatrix[i] = new float[16];
			
			rotationMatrix[i] = new float [16];
			translationMatrix[i] = new float [16];			
		}
		
		for (int i = 0; i < numCaptionTextures; i++) {
			caption[i] = new Caption();
		}
		
		object[0] = new RawObject(context, R.drawable.surfobj);
		object[1] = new RawObject(context, R.drawable.tableobj);
		object[2] = new RawObject(context, R.drawable.chairobj);
		
		// set black background
		glClearColor(0f, 0f, 0f, 0f);
		
		//block = new Block();
		
		textureProgram = new TextureShaderProgram(context);
		transparentProgram = new TransparentTextureShaderProgram(context);
		colorProgram = new ColorShaderProgram(context);
		objectProgram = new ObjectShaderProgram(context);
		
		textures[0] = TextureHelper.loadTexture(context, R.drawable.cory);	
		textures[1] = TextureHelper.loadTexture(context, R.drawable.soda);	
		textures[2] = TextureHelper.loadTexture(context, R.drawable.surf);
		textures[3] = TextureHelper.loadTexture(context, R.drawable.table);
		textures[4] = TextureHelper.loadTexture(context, R.drawable.chair);		
		compassTexture = TextureHelper.loadTexture(context, R.drawable.compass);
	}
	
	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		glViewport( 0, 0, width, height );
		MatrixHelper.perspectiveM(projectionMatrix, 45, (float) width / (float) height, near, far);
		setLookAtM(viewMatrix, 0, 0, 0, 0, 0, 0, -1, 0, 1, 0);
		setLookAtM(compassViewMatrix, 0, 0, 0, 0, 0, 0, -1, 0, 1, 0);
		for (int i = 0; i < numObjects; i++) {
			setIdentityM(modelMatrix[i], 0);
			setIdentityM(translationMatrix[i], 0);
			setIdentityM(rotationMatrix[i], 0);
		}
		setIdentityM(compassRotationMatrix, 0);
		setIdentityM(compassTranslationMatrix,0);
		translateM(compassTranslationMatrix, 0, 0f, 0f, -zcompass);
		
		translateM(translationMatrix[0], 0, 0f, 0f, -ztranslate);
		translateM(translationMatrix[1], 0, 0f, 0f, -ztranslate);
		translateM(translationMatrix[2], 0, 0f, -2f, -ztranslate);
		translateM(translationMatrix[3], 0, 0f, 0f, -4);
		translateM(translationMatrix[4], 0, 0f, -2f, -ztranslate);
		rotateM(translationMatrix[5], 0, 90, 0, 1, 0);
		translateM(translationMatrix[5], 0, 0f, 0f, -zpuck);
		
		rotateM(rotationMatrix[0], 0, 0, 0, 1, 0);
		rotateM(rotationMatrix[1], 0, 0, 0, 1, 0);
		rotateM(rotationMatrix[2], 0, 90, 0, 1, 0);
		rotateM(rotationMatrix[3], 0, 120, 0, 1, 0);
		rotateM(rotationMatrix[4], 0, 45, 0, 1, 0);
		rotateM(rotationMatrix[5], 0, -135, 1, 0, 0);
		rotateM(rotationMatrix[5], 0,0, 0, 0, 1);

		for (int i = 0; i < numObjects; i++) {
			multiplyMM(modelMatrix[i], 0, rotationMatrix[i], 0, translationMatrix[i], 0);
			multiplyMM(modelViewMatrix[i], 0, viewMatrix, 0, modelMatrix[i], 0);
			multiplyMM(finalMatrix[i], 0, projectionMatrix, 0, modelViewMatrix[i], 0);
		}
		multiplyMM(compassModelMatrix, 0, compassTranslationMatrix, 0, compassRotationMatrix, 0);
		multiplyMM(compassModelViewMatrix, 0, compassViewMatrix, 0, compassModelMatrix, 0);
		multiplyMM(compassFinalMatrix, 0, projectionMatrix, 0, compassModelViewMatrix, 0);
	}
	
	@Override
	public void onDrawFrame(GL10 glUnused) {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

//		glDisable(GL_BLEND);

		
		textureProgram.useProgram();
		textureProgram.setUniforms(finalMatrix[0], textures[0]);
		caption[0].bindData(textureProgram);
		caption[0].draw();
		
		textureProgram.setUniforms(finalMatrix[1], textures[1]);
		caption[1].bindData(textureProgram);
		caption[1].draw();
		
		objectProgram.useProgram();
		objectProgram.setUniforms(finalMatrix[2], textures[2]);
		object[0].bindData(objectProgram);
		object[0].draw();
		
		objectProgram.setUniforms(finalMatrix[3], textures[3]);
		object[1].bindData(objectProgram);
		object[1].draw();
		
		objectProgram.setUniforms(finalMatrix[4], textures[4]);
		object[2].bindData(objectProgram);
		object[2].draw();
		
		colorProgram.useProgram();
		colorProgram.setUniforms(finalMatrix[5], 0f, 0.40f, 0.20f, 0.3f);
		puck.bindData(colorProgram);
		puck.draw();
		
		transparentProgram.useProgram();
		transparentProgram.setUniforms(compassFinalMatrix, compassTexture);
		compass.bindData(transparentProgram);
		compass.draw();

	}

	public void sensorUpdate(float mAzimuth, float roll, float pitch, double dx, double dy, double dz) {
		if (mode == Mode.MoveObject) {
			for (int i=0; i<numObjects; i++) {
				rotateM(rotationMatrix[i], 0, mAzimuth, 0f, 1f, 0f);
				rotateM(rotationMatrix[i], 0, pitch, 1f, 0f, 0f);
			}
		}
		else if (mode == Mode.MoveCamera) {
			x = (float) Math.cos(Math.toRadians(mAzimuth));
			z = (float) Math.sin(Math.toRadians(mAzimuth));
			y = (float) Math.tan(Math.toRadians(-pitch));
			forward[0] = x;
			forward[1] = z;
			forward[2] = y;
			normalize(forward);
			float a = (float) Math.cos(Math.toRadians(-roll-90));
			float b = (float) Math.sin(Math.toRadians(-roll-90));
			if (-pitch > 0.1) {				
				 up_portrait[0] = -x;
				 up_portrait[1] = 1.0f/y;
				 up_portrait[2] = -z*a;
			}
			else if (-pitch < 0.1) {
				up_portrait[0] = x;
				up_portrait[1] = -1.0f/y;
				up_portrait[2] = z*a;			
			}			
			else {
				up_portrait[0] = 0;
				up_portrait[1] = 1.0f;
				up_portrait[2] = 0;			
			}
			normalize(up_portrait);
			up_landscape[0] = z;
			up_landscape[1] = 0;
			up_landscape[2] = -x;
			normalize(up_landscape);
			
			setLookAtM(viewMatrix, 0, (float)dx + cameradx, (float)0 + camerady, (float)dz+cameradz, (float)(dx+x+cameradx), (float)(dy+y+camerady), (float)(dz+z+cameradz), a*up_portrait[0]+b*up_landscape[0],
					a*up_portrait[1]+b*up_landscape[1], a*up_portrait[2]+b*up_landscape[2]);
			
			if (-pitch > 0.1) {				
				 up_portrait[0] = 0;
				 up_portrait[1] = 1.0f/y;
				 up_portrait[2] = a;
			}
			else if (-pitch < 0.1) {
				up_portrait[0] = 0;
				up_portrait[1] = -1.0f/y;
				up_portrait[2] = -a;			
			}			
			else {
				up_portrait[0] = 0;
				up_portrait[1] = 1.0f;
				up_portrait[2] = 0;			
			}
			normalize(up_portrait);
			setLookAtM(compassViewMatrix, 0, 0f, 0f, 0f, 0f, y, -1f,   a*up_portrait[0]-b,
					a*up_portrait[1], a*up_portrait[2]);
			
		}	
		for (int i = 0; i < numObjects; i++) {
			multiplyMM(modelMatrix[i], 0, rotationMatrix[i], 0, translationMatrix[i], 0);
			multiplyMM(modelViewMatrix[i], 0, viewMatrix, 0, modelMatrix[i], 0);
			multiplyMM(finalMatrix[i], 0, projectionMatrix, 0, modelViewMatrix[i], 0);	
		}
		multiplyMM(modelMatrix[5], 0,  translationMatrix[5], 0, rotationMatrix[5], 0);
		multiplyMM(modelViewMatrix[5], 0, viewMatrix, 0, modelMatrix[5], 0);
		multiplyMM(finalMatrix[5], 0, projectionMatrix, 0, modelViewMatrix[5], 0);	
		
		setIdentityM(compassRotationMatrix, 0);
		rotateM(compassRotationMatrix, 0, mAzimuth, 0f, 1f, 0f);
		multiplyMM(compassModelMatrix, 0, compassTranslationMatrix, 0, compassRotationMatrix, 0);
		multiplyMM(compassModelViewMatrix, 0, compassViewMatrix, 0, compassModelMatrix, 0);
		multiplyMM(compassFinalMatrix, 0, projectionMatrix, 0, compassModelViewMatrix, 0);
	}
		
	void normalize(float [] a) {
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			sum+=a[i]*a[i];
		}
		sum = Math.sqrt(sum);
		for (int i = 0; i < a.length; i++) {
			a[i]=(float) (a[i]/sum);
		}
		
	}
	
	public void moveCamera(float dx, float dy) {
		cameradx = cameradx - forward[0]*dy - up_landscape[0]*dx;
		camerady = camerady - forward[1]*dy - up_landscape[1]*dx;
		cameradz = cameradz - forward[2]*dy - up_landscape[2]*dx;
	}
	
    public void moveObject(float forward, float right) {
    }
    
    public void setObject(int id, float angleX, float angleY, float angleZ) {
		setIdentityM(translationMatrix[id+2], 0);
		setIdentityM(rotationMatrix[id+2], 0);
    	translateM(translationMatrix[id+2], 0, 0, (float) (busz*Math.tan(Math.toRadians(angleY))), -busz);
    	rotateM(rotationMatrix[id+2], 0, 270, 1, 1, 0);
    	multiplyMM(modelViewMatrix[id+2], 0, translationMatrix[id+2], 0, rotationMatrix[id+2], 0);
    	
    	setIdentityM(rotationMatrix[id+2], 0);
    	rotateM(rotationMatrix[id+2], 0, (-angleX-90)%360, 0, 1, 0);
    	multiplyMM(modelMatrix[id+2], 0, rotationMatrix[id+2], 0, modelViewMatrix[id+2], 0);
    	translateM(modelMatrix[id+2], 0, -cameradx, -camerady, -cameradz);
		multiplyMM(modelViewMatrix[id+2], 0, viewMatrix, 0, modelMatrix[id+2], 0);
		multiplyMM(finalMatrix[id+2], 0, projectionMatrix, 0, modelViewMatrix[id+2], 0);	
    	
    }

}
