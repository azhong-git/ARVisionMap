package com.example.util;

import static android.opengl.GLES20.glUseProgram;
import android.content.Context;

public class Program {

	// for vertex and fragment shaders
	
	protected static final String A_POSITION = "a_Position";
	protected static final String A_TEXTURE_COORD = "a_TextureCoord";
	protected static final String U_MATRIX = "u_Matrix";
	protected static final String U_TEXTURE_UNIT = "u_TextureUnit";
	protected static final String U_COLOR = "u_Color";
	protected static final String A_COLOR = "a_Color";
	protected final int program;
	
	protected Program(Context context, int vertexShaderResourceId, int fragmentShaderResourceId) {
		// read shader source
		String vertexShaderSource = TextResourceReader.readTextFileFromResource(context, vertexShaderResourceId);
		String fragmentShaderSource = TextResourceReader.readTextFileFromResource(context, fragmentShaderResourceId);
		
		program = ShaderHelper.buildProgram(vertexShaderSource, fragmentShaderSource);		
	}
	
	public void useProgram() {
		glUseProgram(program);
	}
}