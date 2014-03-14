package com.example.objects;

import java.util.ArrayList;
import java.util.List;

import com.example.util.Geometry.Cone;
import com.example.util.Geometry.Cylinder;
import com.example.util.Geometry.Circle;

import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glDrawArrays;

public class ObjectBuilder {
	private static final int FLOATS_PER_VERTEX = 3;
	private final float [] vertexData;
	private final List<DrawCommand> drawList = new ArrayList<DrawCommand>();
	private int offset = 0;
	
	private ObjectBuilder(int sizeInVertices) {
		vertexData = new float [sizeInVertices*(FLOATS_PER_VERTEX+1)];
	}
	
	private static int sizeOfCircleInVertices(int numPoints) {
		return 1+(numPoints+1);
	}
	
	private static int sizeOfOpenCylinderInVertices(int numPoints) {
		return (numPoints + 1) *2;
	}
	
	static class GeneratedData {
		final float [] vertexData;
		final List<DrawCommand> drawList;
		
		GeneratedData(float [] vertexData, List<DrawCommand> drawList) {
			this.vertexData = vertexData;
			this.drawList = drawList;
		}
	}
	
	private GeneratedData build() {
		return new GeneratedData(vertexData, drawList);
	}
	
	private void appendCircle(Circle circle, int numPoints) {
		final int startVertex = offset / (FLOATS_PER_VERTEX+1);
		final int numVertices = sizeOfCircleInVertices(numPoints);
		
		// center of the circle
		vertexData[offset++] = circle.center.x;
		vertexData[offset++] = circle.center.y;
		vertexData[offset++] = circle.center.z;
		vertexData[offset++] = 1;
		
		for (int i =0; i <= numPoints; i++) {
			float angleInRadians = ((float) i / (float) numPoints) * (float) Math.PI * 2f; 
			
			vertexData[offset++] = (float) (circle.center.x + circle.radius*Math.cos(angleInRadians));
			vertexData[offset++] = circle.center.y;
			vertexData[offset++] = (float) (circle.center.z+circle.radius*Math.sin(angleInRadians));
			vertexData[offset++] = 1;
		}
		
		drawList.add(new DrawCommand() {
			@Override
			public void draw() {
				glDrawArrays(GL_TRIANGLE_FAN, startVertex, numVertices);
			}}
			);
	}
	
	static interface DrawCommand {
		void draw();
	}
	
	private void appendCone(Cone cone, int numPoints) {
		final int startVertex = offset / (FLOATS_PER_VERTEX+1);
		final int numVertices = sizeOfCircleInVertices(numPoints);
		
		// center of the circle
		vertexData[offset++] = cone.center.x;
		vertexData[offset++] = cone.center.y+cone.height;
		vertexData[offset++] = cone.center.z;
		vertexData[offset++] = 0.8f;
		
		for (int i =0; i <= numPoints; i++) {
			float angleInRadians = ((float) i / (float) numPoints) * (float) Math.PI * 2f; 
			
			vertexData[offset++] = (float) (cone.center.x + cone.radius*Math.cos(angleInRadians));
			vertexData[offset++] = cone.center.y;
			vertexData[offset++] = (float) (cone.center.z+cone.radius*Math.sin(angleInRadians));
			vertexData[offset++] = 0.8f;
		}
		
		drawList.add(new DrawCommand() {
			@Override
			public void draw() {
				glDrawArrays(GL_TRIANGLE_FAN, startVertex, numVertices);
			}}
			);
	}
	
	private void appendOpenCylinder(Cylinder cylinder, int numPoints) {
		final int startVertex = offset / (FLOATS_PER_VERTEX+1);
		final int numVertices = sizeOfOpenCylinderInVertices(numPoints);
		final float yStart = cylinder.center.y - cylinder.height/2f;
		final float yEnd = cylinder.center.y + cylinder.height/2f; 
		for (int i = 0; i<=numPoints; i++) {
			float angleInRadians = ((float) i / (float) numPoints) * (float) Math.PI * 2f; 
			float x = (float) (cylinder.center.x+cylinder.radius * Math.cos(angleInRadians));
			float z = (float) (cylinder.center.z+cylinder.radius * Math.sin(angleInRadians));
			
			vertexData[offset++] = x;
			vertexData[offset++] = yStart;
			vertexData[offset++] = z;
			vertexData[offset++] = 1f;
			vertexData[offset++] = x;
			vertexData[offset++] = yEnd;
			vertexData[offset++] = z;
			vertexData[offset++] = 1f;
		}
		drawList.add(new DrawCommand() {
			@Override
			public void draw() {
				glDrawArrays(GL_TRIANGLE_STRIP, startVertex, numVertices);
			}
		});
	}
	
	static GeneratedData createPuck(Cylinder puck, int numPoints) {
		int size = sizeOfCircleInVertices(numPoints) + sizeOfOpenCylinderInVertices(numPoints);
		ObjectBuilder builder = new ObjectBuilder(size);
		Circle puckTop = new Circle (puck.center.translateY(puck.height/2f), puck.radius);
		builder.appendCircle(puckTop, numPoints);
		builder.appendOpenCylinder(puck, numPoints);
		return builder.build();
	}
	
	static GeneratedData createArrow(Cylinder puck, Cone top, int numPoints) {
		int size = (sizeOfCircleInVertices(numPoints) + sizeOfOpenCylinderInVertices(numPoints));
		//int size= sizeOfCircleInVertices(numPoints);
		ObjectBuilder builder = new ObjectBuilder(size);
		builder.appendCone(top, numPoints);
		builder.appendOpenCylinder(puck, numPoints);
		return builder.build();
	}
}