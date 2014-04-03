package com.example.augmentedreality;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_ONE;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glViewport;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.setLookAtM;
import static android.opengl.Matrix.translateM;

import java.util.Scanner;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLSurfaceView.Renderer;

import com.example.objects.Arrow;
import com.example.objects.Caption;
import com.example.objects.RawObject;
import com.example.objects.Square;
import com.example.openglbasics.R;
import com.example.util.ColorShaderProgram;
import com.example.util.MatrixHelper;
import com.example.util.ObjectShaderProgram;
import com.example.util.TextResourceReader;
import com.example.util.TextureHelper;
import com.example.util.TextureShaderProgram;
import com.example.util.TransparentTextureShaderProgram;

public class ARVisionRenderer implements Renderer {

	private final Context context;

	// ---------------------- Matrices used for transformations -----------------------
	// general project matrix and view matrix
	private float[] sensorProjectionMatrix;
	private float[] sensorViewMatrix;

	// model, view, final matrices for each object
	private float[][] modelMatrix;
	private float[][] modelViewMatrix;
	private float[][] finalMatrix;
	private float[][] rotationMatrix;
	private float[][] translationMatrix;

	// separate matrices for compass (compass never moves out of screen)
	private float[] compassModelMatrix;
	private float[] compassRotationMatrix;
	private float[] compassTranslationMatrix;
	private float[] compassViewMatrix;
	private float[] compassModelViewMatrix;
	private float[] compassFinalMatrix;

	// helper matrices for setting up camera lookat
	float[] forward;
	float[] up_portrait;
	float[] up_landscape;
	//---------------------------------------------------------------------------------
	// ---------------------------- Location parameters -------------------------------
	// projection matrix parameters
	private final float far = 10f;
	private final float near = 0.2f;

	// distance on the z axis
	//private final float[] zdis = new float[MAXNUM_TEXTURES];
	
	private int [] objectType;
	private int [] objectTexture;
	private float [][] objLoc;
	private float [][] objRot;

	private final float busz = 1.0f;
	private final float zcompass = 1f;

	// camera location
	private float cameradx = 0;
	private float camerady = 0;
	private float cameradz = 0;

	// other parameters
	private float x = 0;
	private float y = 0;
	private float z = 0;
	//---------------------------------------------------------------------------------
	//------------------------------- Status variables --------------------------------
	private enum Mode {
		MoveObject, MoveCamera;
	}

	private Mode mode = Mode.MoveCamera;
	
	public enum GraphicsStatus {
		Loading, Ready;
	}
	
	public static GraphicsStatus GLStatus = GraphicsStatus.Loading;
	//---------------------------------------------------------------------------------	
	//------------------------------- Objects and textures ----------------------------
	// number of captions (w/ texture)
	private int numCaptions;
	// number of raw objects (w/ texture)
	private int numRawObjects;
	// number of arrows (w/o texture)
	private int numArrows;
	
	// number of objects (numCaptions + numRawObjects + numArrows)
	private int numObjects;
	// number of textures (numCaptions + numRawObjects)
	private int numTextures;

	// objects with textures
	private int [] textures;
	private Caption[] caption;
	private int [] captionTexture = {R.drawable.cory, R.drawable.soda};
	
	private RawObject[] object;
	private int [] rawObjTextureMap = {R.drawable.surfobj, R.drawable.tableobj, R.drawable.chairobj};
	private int [] rawObjTexture = {R.drawable.surf, R.drawable.table, R.drawable.chair};
	
	// compass object
	private Square compass;
	private int compassTexture;

	// object without texture
	private Arrow[] arrow;
	//---------------------------------------------------------------------------------
	//------------------------------- Shader programs ---------------------------------
	private TextureShaderProgram textureProgram;
	private TransparentTextureShaderProgram transparentProgram;
	private ColorShaderProgram colorProgram;
	private ObjectShaderProgram objectProgram;
	//---------------------------------------------------------------------------------
	
	public ARVisionRenderer(Context context) {
		this.context = context;
		String mapInput = TextResourceReader.readTextFileFromResource(context, R.raw.map);
		Scanner scan = new Scanner(mapInput);
		
		// reading parameters from input map
		numCaptions = TextResourceReader.readNextInt(scan);
		numRawObjects = TextResourceReader.readNextInt(scan);
		numArrows = TextResourceReader.readNextInt(scan);
		numObjects = numCaptions + numRawObjects + numArrows;
		numTextures = numCaptions + numRawObjects;
		
		// initialize transformation matrices
		initMatrices();

		
		objectType = new int [numObjects];
		objectTexture = new int [numObjects];
		objLoc = new float [numObjects][3];
		objRot = new float [numObjects][3];
		
		for (int i = 0; i < numObjects; i++) {
			objectType[i] = TextResourceReader.readNextInt(scan);
			// caption or raw object
			if (objectType[i] != 2) {
				objectTexture[i] = TextResourceReader.readNextInt(scan);
			}
			for (int j = 0; j < 3; j++)
				objLoc[i][j] = TextResourceReader.readNextFloat(scan);
			for (int j = 0; j < 3; j++)
				objRot[i][j] = TextResourceReader.readNextFloat(scan);
		}
	}

	// initialize transformation matrices
	private void initMatrices() {
		// --------------------------- matrices init -------------------------------
		// general project matrix and view matrix
		sensorProjectionMatrix = new float[16];
		sensorViewMatrix = new float[16];

		// model, view, final matrices for each object
		modelMatrix = new float[numObjects][16];
		modelViewMatrix = new float[numObjects][16];
		finalMatrix = new float[numObjects][16];
		rotationMatrix = new float[numObjects][16];
		translationMatrix = new float[numObjects][16];

		// separate matrices for compass (compass never moves out of screen)
		compassModelMatrix = new float[16];
		compassRotationMatrix = new float[16];
		compassTranslationMatrix = new float[16];
		compassViewMatrix = new float[16];
		compassModelViewMatrix = new float[16];
		compassFinalMatrix = new float[16];

		// helper matrices for setting up camera lookat
		forward = new float[3];
		up_portrait = new float[3];
		up_landscape = new float[3];
	}
	
	// initialize objects including captions, raw objects and arrows
	private void initObjects () {
		// initialize captions
		caption = new Caption[numCaptions];
		int i = 0;
		for (i = 0; i < numCaptions; i++) {
			caption[i] = new Caption();
		}

		// initialize raw objects
		object = new RawObject[numRawObjects];
		for (i = 0; i < numRawObjects; i++) {
			object[i] = new RawObject(context, rawObjTextureMap[objectTexture[i+numCaptions]]);
		}
		
		// initialize arrows
		arrow = new Arrow[numArrows];
		for (i = 0; i < numArrows; i++) {
			arrow[i] = new Arrow(1, 2, (float) 0.5, 1, 32);
		}
		
		// initialize compass
		compass = new Square();
		
		// bind textures
		textures = new int[numTextures];
	}
	
	
	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
		// set black background
		glClearColor(0f, 0f, 0f, 0f);

		// setting up programs
		textureProgram = new TextureShaderProgram(context);
		transparentProgram = new TransparentTextureShaderProgram(context);
		colorProgram = new ColorShaderProgram(context);
		objectProgram = new ObjectShaderProgram(context);
		
		// initialize objects
		initObjects();
		
		// bind object texture
		int i = 0;
		for (; i < numCaptions; i++)
			textures[i] = TextureHelper.loadTexture(context, captionTexture[objectTexture[i]]);
		for (; i < numTextures; i++) 
			textures[i] = TextureHelper.loadTexture(context, rawObjTexture[objectTexture[i]]);

		// bind compass texture
		compassTexture = TextureHelper.loadTexture(context, R.drawable.compass);
		
		// objects successfully loaded
		GLStatus = GraphicsStatus.Ready;
				
	}

	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		glViewport(0, 0, width, height);
		MatrixHelper.perspectiveM(sensorProjectionMatrix, 45, (float) width
				/ (float) height, near, far);
		setLookAtM(sensorViewMatrix, 0, 0, 0, 0, 0, 0, -1, 0, 1, 0);
		setLookAtM(compassViewMatrix, 0, 0, 0, 0, 0, 0, -1, 0, 1, 0);
		int i = 0;
		for (; i < numObjects; i++) {
			setIdentityM(modelMatrix[i], 0);
			setIdentityM(translationMatrix[i], 0);
			setIdentityM(rotationMatrix[i], 0);
		}
		setIdentityM(compassRotationMatrix, 0);
		setIdentityM(compassTranslationMatrix, 0);
		translateM(compassTranslationMatrix, 0, 0f, 0f, -zcompass);

		for (i = 0; i < numTextures; i++) {
			translateM(translationMatrix[i], 0, objLoc[i][0], objLoc[i][1], objLoc[i][2]);
			rotateM(rotationMatrix[i], 0, objRot[i][1], 0, 1, 0);
			multiplyMM(modelMatrix[i], 0, rotationMatrix[i], 0,
					translationMatrix[i], 0);
			multiplyMM(modelViewMatrix[i], 0, sensorViewMatrix, 0,
					modelMatrix[i], 0);
			multiplyMM(finalMatrix[i], 0, sensorProjectionMatrix, 0,
					modelViewMatrix[i], 0);
		}

		for (; i < numObjects; i++) {
			rotateM(translationMatrix[i], 0, objRot[i][1], 0, 1, 0);
			translateM(translationMatrix[i], 0, objLoc[i][0], objLoc[i][1], objLoc[i][2]);
			rotateM(rotationMatrix[i], 0, objRot[i][0], 1, 0, 0);
			//rotateM(rotationMatrix[i], 0, objRot[i][1], 0, 1, 0);
			//rotateM(rotationMatrix[i], 0, objRot[i][1], 0, 0, 1);
			multiplyMM(modelMatrix[i], 0, translationMatrix[i], 0,
					rotationMatrix[i], 0);
			multiplyMM(modelViewMatrix[i], 0, sensorViewMatrix, 0,
					modelMatrix[i], 0);
			multiplyMM(finalMatrix[i], 0, sensorProjectionMatrix, 0,
					modelViewMatrix[i], 0);
		}

		multiplyMM(compassModelMatrix, 0, compassTranslationMatrix, 0,
				compassRotationMatrix, 0);
		multiplyMM(compassModelViewMatrix, 0, compassViewMatrix, 0,
				compassModelMatrix, 0);
		multiplyMM(compassFinalMatrix, 0, sensorProjectionMatrix, 0,
				compassModelViewMatrix, 0);
	}

	@Override
	public void onDrawFrame(GL10 glUnused) {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

		int i = 0;
		textureProgram.useProgram();
		for (; i < numCaptions; i++) {
			textureProgram.setUniforms(finalMatrix[i], textures[i]);
			caption[i].bindData(textureProgram);
			caption[i].draw();
		}

		objectProgram.useProgram();
		for (; i < numTextures; i++) {
			objectProgram.setUniforms(finalMatrix[i], textures[i]);
			object[i-numCaptions].bindData(objectProgram);
			object[i-numCaptions].draw();
		}

		colorProgram.useProgram();
		for (; i < numObjects; i++) {
			colorProgram.setUniforms(finalMatrix[i], 0f, 0.40f, 0.20f, 0.3f);
			arrow[i-numTextures].bindData(colorProgram);
			arrow[i-numTextures].draw();
		}
		
		transparentProgram.useProgram();
		transparentProgram.setUniforms(compassFinalMatrix, compassTexture);
		compass.bindData(transparentProgram);
		compass.draw();

	}

	public void sensorUpdate(float mAzimuth, float roll, float pitch,
			double dx, double dy, double dz) {
		if (mode == Mode.MoveObject) {
			for (int i = 0; i < numObjects; i++) {
				rotateM(rotationMatrix[i], 0, mAzimuth, 0f, 1f, 0f);
				rotateM(rotationMatrix[i], 0, pitch, 1f, 0f, 0f);
			}
		} else if (mode == Mode.MoveCamera) {
			x = (float) Math.cos(Math.toRadians(mAzimuth));
			z = (float) Math.sin(Math.toRadians(mAzimuth));
			y = (float) Math.tan(Math.toRadians(-pitch));
			forward[0] = x;
			forward[1] = z;
			forward[2] = y;
			normalize(forward);
			float a = (float) Math.cos(Math.toRadians(-roll - 90));
			float b = (float) Math.sin(Math.toRadians(-roll - 90));
			if (-pitch > 0.1) {
				up_portrait[0] = -x;
				up_portrait[1] = 1.0f / y;
				up_portrait[2] = -z * a;
			} else if (-pitch < 0.1) {
				up_portrait[0] = x;
				up_portrait[1] = -1.0f / y;
				up_portrait[2] = z * a;
			} else {
				up_portrait[0] = 0;
				up_portrait[1] = 1.0f;
				up_portrait[2] = 0;
			}
			normalize(up_portrait);
			up_landscape[0] = z;
			up_landscape[1] = 0;
			up_landscape[2] = -x;
			normalize(up_landscape);

			setLookAtM(sensorViewMatrix, 0, (float) dx + cameradx, (float) 0
					+ camerady, (float) dz + cameradz,
					(float) (dx + x + cameradx), (float) (dy + y + camerady),
					(float) (dz + z + cameradz), a * up_portrait[0] + b
							* up_landscape[0], a * up_portrait[1] + b
							* up_landscape[1], a * up_portrait[2] + b
							* up_landscape[2]);

			if (-pitch > 0.1) {
				up_portrait[0] = 0;
				up_portrait[1] = 1.0f / y;
				up_portrait[2] = a;
			} else if (-pitch < 0.1) {
				up_portrait[0] = 0;
				up_portrait[1] = -1.0f / y;
				up_portrait[2] = -a;
			} else {
				up_portrait[0] = 0;
				up_portrait[1] = 1.0f;
				up_portrait[2] = 0;
			}
			normalize(up_portrait);
			setLookAtM(compassViewMatrix, 0, 0f, 0f, 0f, 0f, y, -1f, a
					* up_portrait[0] - b, a * up_portrait[1], a
					* up_portrait[2]);

		}
		int i = 0;
		for (; i < numTextures; i++) {
			multiplyMM(modelMatrix[i], 0, rotationMatrix[i], 0,
					translationMatrix[i], 0);
			multiplyMM(modelViewMatrix[i], 0, sensorViewMatrix, 0,
					modelMatrix[i], 0);
			multiplyMM(finalMatrix[i], 0, sensorProjectionMatrix, 0,
					modelViewMatrix[i], 0);
		}
		
		for (; i < numObjects; i++) {
			multiplyMM(modelMatrix[i], 0, translationMatrix[i], 0,
					rotationMatrix[i], 0);
			multiplyMM(modelViewMatrix[i], 0, sensorViewMatrix, 0, modelMatrix[i],
					0);
			multiplyMM(finalMatrix[i], 0, sensorProjectionMatrix, 0,
					modelViewMatrix[i], 0);
		}

		setIdentityM(compassRotationMatrix, 0);
		rotateM(compassRotationMatrix, 0, mAzimuth, 0f, 1f, 0f);
		multiplyMM(compassModelMatrix, 0, compassTranslationMatrix, 0,
				compassRotationMatrix, 0);
		multiplyMM(compassModelViewMatrix, 0, compassViewMatrix, 0,
				compassModelMatrix, 0);
		multiplyMM(compassFinalMatrix, 0, sensorProjectionMatrix, 0,
				compassModelViewMatrix, 0);
	}

	void normalize(float[] a) {
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			sum += a[i] * a[i];
		}
		sum = Math.sqrt(sum);
		for (int i = 0; i < a.length; i++) {
			a[i] = (float) (a[i] / sum);
		}

	}

	public void moveCamera(float dx, float dy) {
		cameradx = cameradx - forward[0] * dy - up_landscape[0] * dx;
		camerady = camerady - forward[1] * dy - up_landscape[1] * dx;
		cameradz = cameradz - forward[2] * dy - up_landscape[2] * dx;
	}

//	public void moveObject(float forward, float right) {
//	}

	public void setObject(int id, float angleX, float angleY, float angleZ) {
		setIdentityM(translationMatrix[id + 2], 0);
		setIdentityM(rotationMatrix[id + 2], 0);
		translateM(translationMatrix[id + 2], 0, 0,
				(float) (busz * Math.tan(Math.toRadians(angleY))), -busz);
		rotateM(rotationMatrix[id + 2], 0, 270, 1, 1, 0);
		multiplyMM(modelViewMatrix[id + 2], 0, translationMatrix[id + 2], 0,
				rotationMatrix[id + 2], 0);

		setIdentityM(rotationMatrix[id + 2], 0);
		rotateM(rotationMatrix[id + 2], 0, (-angleX - 90) % 360, 0, 1, 0);
		multiplyMM(modelMatrix[id + 2], 0, rotationMatrix[id + 2], 0,
				modelViewMatrix[id + 2], 0);
		translateM(modelMatrix[id + 2], 0, -cameradx, -camerady, -cameradz);
		multiplyMM(modelViewMatrix[id + 2], 0, sensorViewMatrix, 0,
				modelMatrix[id + 2], 0);
		multiplyMM(finalMatrix[id + 2], 0, sensorProjectionMatrix, 0,
				modelViewMatrix[id + 2], 0);

	}

}
