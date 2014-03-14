package com.example.util;

import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUniformMatrix4fv;
import android.content.Context;

import com.example.openglbasics.R;

public class ColorShaderProgram extends Program {
	private final int uMatrixLocation;
	private final int uColorLocation;	
	private final int aPositionLocation;
	private final int aColorLocation;
	
	public ColorShaderProgram(Context context) {
		super(context, R.raw.simple_vertex_shader, R.raw.simple_fragment_shader);
		
		uColorLocation = glGetUniformLocation(program, U_COLOR);
		uMatrixLocation = glGetUniformLocation(program, U_MATRIX);
		aPositionLocation = glGetAttribLocation(program, A_POSITION);
		aColorLocation = glGetAttribLocation(program, A_COLOR);
		
	}
	
	public void setUniforms(float [] matrix, float r, float g, float b, float gamma) {
		glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);
		glUniform4f(uColorLocation,r,g,b,gamma);
	}
	
	public int getPositionAttributeLocation() {
		return aPositionLocation;
	}
	
	public int getColorAttributeLocation() {
		return aColorLocation;
	}
}