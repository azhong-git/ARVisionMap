package com.example.objects;

import java.util.List;
import com.example.objects.ObjectBuilder.DrawCommand;
import com.example.objects.ObjectBuilder.GeneratedData;
import com.example.util.ColorShaderProgram;
import com.example.util.Geometry.Cylinder;
import com.example.util.Geometry.Point;
import com.example.data.VertexArray;

public class Puck {
	private static final int POSITION_COMPONENT = 3;
	public final float radius, height;
	
	private final VertexArray vertexArray;
	private final List<DrawCommand> drawList;
	
	public Puck(float radius, float height, int numPoints) {
		GeneratedData generatedData = ObjectBuilder.createPuck(new Cylinder(new Point(0f, 0f, 0f), radius, height),
				numPoints);
		this.radius = radius;
		this.height = height;
		vertexArray = new VertexArray(generatedData.vertexData);
		drawList = generatedData.drawList;		
	}
	
	public void bindData(ColorShaderProgram colorProgram) {
		vertexArray.setVertexAttribPointer(0, colorProgram.getPositionAttributeLocation(), POSITION_COMPONENT,
				0);
	}
	
	public void draw() {
		for (DrawCommand i: drawList) {
			i.draw();
		}
	}
	
}