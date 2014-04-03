package com.example.objects;

import static android.opengl.GLES20.GL_BACK;
import static android.opengl.GLES20.GL_CCW;
import static android.opengl.GLES20.GL_CULL_FACE;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.glCullFace;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glFrontFace;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import android.content.Context;
import android.content.res.Resources;

import com.example.data.VertexArray;
import com.example.util.ObjectShaderProgram;

public class RawObject {
	private static final int POSITION_COMPONENT_COUNT = 3;
	private static final int NORMAL_COMPONENT_COUNT = 3;
	private static final int TEXTURE_COORD_COMPONENT_COUNT = 2;
	private final VertexArray vertexArray;
	private int faces;
	
	public RawObject(Context context, int resourceId) {
		try {
			InputStream inputStream = context.getResources().openRawResource(resourceId);
			Scanner scan = new Scanner(inputStream);
			faces = scan.nextInt();
			int size = 3 * faces * (POSITION_COMPONENT_COUNT + NORMAL_COMPONENT_COUNT + TEXTURE_COORD_COMPONENT_COUNT);			
			float [] temp = new float [size];
			for (int i = 0; i < size; i++)
				temp[i] = scan.nextFloat();
//			float [] vertexData = new float [size];
//			for (int i = 0; i < faces*3; i=i+1) {
//				vertexData[i*8] = temp[i*3];
//				vertexData[i*8+1] = temp[i*3+1];
//				vertexData[i*8+2] = temp[i*3+2];
//				vertexData[i*8+3] = temp[faces*9+i*3];
//				vertexData[i*8+4] = temp[faces*9+i*3+1];
//				vertexData[i*8+5] = temp[faces*9+i*3+2];
//				vertexData[i*8+6] = temp[faces*18+i*2];
//				vertexData[i*8+7] = temp[faces*18+i*2+1];
//			}
			vertexArray = new VertexArray(temp);
			inputStream.close();
			scan.close();
		}
		catch (IOException e) {
			throw new RuntimeException("Could not open resource no." + resourceId, e);
		}
		catch (Resources.NotFoundException nfe) {
			throw new RuntimeException("Resource not found: " + resourceId, nfe);
		}

	}
	
	public void bindData(ObjectShaderProgram objectProgram) {
//		vertexArray.setVertexAttribPointer(0, 
//					objectProgram.getPositionAttributeLocation(), 
//					POSITION_COMPONENT_COUNT, 
//					32);
//		vertexArray.setVertexAttribPointer(6, 
//				objectProgram.getTextureCoordAttributeLocation(), 
//				TEXTURE_COORD_COMPONENT_COUNT, 
//				32);
		vertexArray.setVertexAttribPointer(0, 
				objectProgram.getPositionAttributeLocation(), 
				POSITION_COMPONENT_COUNT, 
				12);
	vertexArray.setVertexAttribPointer(faces*18, 
			objectProgram.getTextureCoordAttributeLocation(), 
			TEXTURE_COORD_COMPONENT_COUNT, 
			8);
	}
	
	public void draw() {
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
	    glFrontFace(GL_CCW);

	    glEnable(GL_DEPTH_TEST);
		glDrawArrays(GL_TRIANGLES, 0, faces*3);
	}

	
}