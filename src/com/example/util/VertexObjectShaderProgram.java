package com.example.util;

import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUniformMatrix4fv;
import android.content.Context;

import com.example.openglbasics.R;

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
	
	public void setUniforms(float [] matrix, float [] color) {
		glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);
		glUniform4f(uColorLocation,color[0],color[1],color[2],color[3]);
	}
	
	public int getPositionAttributeLocation() {
		return aPositionLocation;
	}
}