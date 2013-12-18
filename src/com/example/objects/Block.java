package com.example.objects;

import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glUniform4f;

import com.example.data.VertexArray;
import com.example.util.SimpleShaderProgram;

public class Block {
	private static final int BYTES_PER_FLOAT = 4;
	private static final int POSITION_COMPONENT_COUNT = 2;
	private static final int STRIDE = POSITION_COMPONENT_COUNT * BYTES_PER_FLOAT;
	
	private static final float [] VERTEX_DATA = {
			// Order of coordinates: X Y
		
			-0.5f,	-1f, 	//0f, 	-1f,
			-0.5f,	0f,		//0f, 	1f,
			0.5f,	0f,		//1f,		1f,
			
			-0.5f,	-1f,		//0f, 	-1f,
			0.5f,	0.0f,	//1f,		1f,
			0.5f,	-1f};		//1f,		-1f};
	
	private final VertexArray vertexArray;
	
	public Block() {
		vertexArray = new VertexArray(VERTEX_DATA);
	}
	
	public void bindData(SimpleShaderProgram simpleProgram) {
		vertexArray.setVertexAttribPointer(0, 
					simpleProgram.getPositionAttributeLocation(), 
					POSITION_COMPONENT_COUNT, 
					STRIDE);
	}
	
	public void draw() {
		glDrawArrays(GL_TRIANGLES,0,6);
	}
}