package com.example.util;

import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUniformMatrix4fv;

import com.example.openglbasics.R;
import android.content.Context;

public class VertexObjectShaderProgram extends Program {
	private final int uMatrixLocation;
	private final int aPositionLocation;
	private final int uColorLocation;
	
	public VertexObjectShaderProgram(Context context) {
		super(context, R.raw.pure_vertex_shader, R.raw.pure_fragment_shader);
		
		uColorLocation = glGetUniformLocation(program, U_COLOR);
		uMatrixLocation = glGetUniformLocation(program, U_MATRIX);
		aPositionLocation = glGetAttribLocation(program, A_POSITION);
	}
	
	public void setUniforms(float [] matrix) {
		glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);
		glUniform4f(uColorLocation,0.2f,0.4f,1f,0.8f);
	}
	
	public int getPositionAttributeLocation() {
		return aPositionLocation;
	}
}