package com.example.objects;

import static android.opengl.GLES20.GL_BACK;
import static android.opengl.GLES20.GL_CCW;
import static android.opengl.GLES20.GL_CULL_FACE;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.glCullFace;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glFrontFace;
import static android.opengl.GLES20.glPolygonOffset;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import android.content.Context;
import android.content.res.Resources;

import com.example.data.VertexArray;
import com.example.util.VertexObjectShaderProgram;

public class RawVertexObject {
	private static final int POSITION_COMPONENT_COUNT = 3;
	private final VertexArray vertexArray;
	private int faces;
	
	public RawVertexObject(Context context, int resourceId) {
		try {
			InputStream inputStream = context.getResources().openRawResource(resourceId);
			Scanner scan = new Scanner(inputStream);
			faces = scan.nextInt();
			int size = 3 * faces * (POSITION_COMPONENT_COUNT);			
			float [] temp = new float [size];
			for (int i = 0; i < size; i++)
				temp[i] = scan.nextFloat();
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
	
	public void bindData(VertexObjectShaderProgram vertexObjectProgram) {
		vertexArray.setVertexAttribPointer(0, 
				vertexObjectProgram.getPositionAttributeLocation(), 
				POSITION_COMPONENT_COUNT, 
				12);
	}
	
	public void draw() {
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
	    glFrontFace(GL_CCW);

	    //glEnable(GL_DEPTH_TEST);

		glPolygonOffset(100, 10);
		glDrawArrays(GL_TRIANGLES, 0, faces*3);
	}

	
}