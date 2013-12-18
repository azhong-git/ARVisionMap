package com.example.util;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glVertexAttribPointer;

import com.example.openglbasics.R;
import android.content.Context;

public class SimpleShaderProgram extends Program {
	private final int uMatrixLocation;
	private final int uColorLocation;	
	private final int aPositionLocation;
	
	public SimpleShaderProgram(Context context) {
		super(context, R.raw.simple_vertex_shader, R.raw.simple_fragment_shader);
		
		uColorLocation = glGetUniformLocation(program, U_COLOR);
		uMatrixLocation = glGetUniformLocation(program, U_MATRIX);
		aPositionLocation = glGetAttribLocation(program, A_POSITION);
		
	}
	
	public void setUniforms(float [] matrix) {
		glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);
		glUniform4f(uColorLocation,0.0f,1.0f,1.0f,1.0f);
	}
	
	public int getPositionAttributeLocation() {
		return aPositionLocation;
	}
}