package com.example.util;

import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;

import com.example.openglbasics.R;
import android.content.Context;

public class ObjectShaderProgram extends Program {
	private final int uMatrixLocation;
	private final int uTextureUnitLocation;
	
	private final int aPositionLocation;
	private final int aTextureCoordLocation;
	
	public ObjectShaderProgram(Context context) {
		super(context, R.raw.object_vertex_shader, R.raw.object_fragment_shader);
		
		uMatrixLocation = glGetUniformLocation(program, U_MATRIX);
		uTextureUnitLocation = glGetUniformLocation(program, U_TEXTURE_UNIT);
		
		aPositionLocation = glGetAttribLocation(program, A_POSITION);
		aTextureCoordLocation = glGetAttribLocation(program, A_TEXTURE_COORD);
	}
	
	public void setUniforms(float [] matrix, int textureId) {
		glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);
		
		glActiveTexture(GL_TEXTURE0);
		
		glBindTexture(GL_TEXTURE_2D, textureId);
		
		glUniform1i(uTextureUnitLocation, 0);
		
	}
	
	public int getPositionAttributeLocation() {
		return aPositionLocation;
	}
	
	public int getTextureCoordAttributeLocation() {
		return aTextureCoordLocation;
	}


}