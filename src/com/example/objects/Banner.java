package com.example.objects;

import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.glDrawArrays;

import com.example.data.VertexArray;
import com.example.util.TransparentTextureShaderProgram;

public class Banner {
	private static final int BYTES_PER_FLOAT = 4;
	private static final int POSITION_COMPONENT_COUNT = 3;
	private static final int TEXTURE_COORD_COMPONENT_COUNT = 2;
	private static final int STRIDE = (POSITION_COMPONENT_COUNT + TEXTURE_COORD_COMPONENT_COUNT) * BYTES_PER_FLOAT;
	
	private static final float [] VERTEX_DATA = {
			// Order of coordinates: X Y S T
		
			-1f,	-0.4f,  -0.15f,		0f, 	0f,
			-1f,	-0.4f, 0.15f,		0f, 	1f,
			1f,	-0.4f, 0.15f,		1f,		1f,
			
			-1f,	-0.4f,  -0.15f,			0f, 	0f,
			1f,	-0.4f, 0.15f,		1f,		1f,
			1f,	-0.4f,	-0.15f,		1f,		0f};

	private final VertexArray vertexArray;
	
	public Banner() {
		vertexArray = new VertexArray(VERTEX_DATA);
	}
	
	public void bindData(TransparentTextureShaderProgram textureProgram) {
		vertexArray.setVertexAttribPointer(0, 
					textureProgram.getPositionAttributeLocation(), 
					POSITION_COMPONENT_COUNT, 
					STRIDE);
		
		vertexArray.setVertexAttribPointer(POSITION_COMPONENT_COUNT, 
					textureProgram.getTextureCoordAttributeLocation(), 
					TEXTURE_COORD_COMPONENT_COUNT, 
					STRIDE);
	}
	
	public void draw() {
		glDrawArrays(GL_TRIANGLES,0,6);
	}
}