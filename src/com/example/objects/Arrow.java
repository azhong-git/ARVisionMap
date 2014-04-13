package com.example.objects;

import java.util.List;
import com.example.objects.ObjectBuilder.DrawCommand;
import com.example.objects.ObjectBuilder.GeneratedData;
import com.example.util.ColorShaderProgram;
import com.example.util.Geometry.Cylinder;
import com.example.util.Geometry.Point;
import com.example.util.Geometry.Cone;
import com.example.data.VertexArray;

public class Arrow {
	private static final int POSITION_COMPONENT = 3;
	public float radiusCone, heightCone, radiusCylinder, heightCylinder;
	
	private VertexArray vertexArray;
	private List<DrawCommand> drawList;
	
	public Arrow() {
	}
	
	public Arrow(float radiusCone, float heightCone, float radiusCylinder, float heightCylinder, int numPoints) {
		GeneratedData generatedData = ObjectBuilder.createArrow(new Cylinder(new Point(0f, 0f, 0f), radiusCylinder, heightCylinder), 
				new Cone(new Point(0f, -heightCylinder/2f, 0f), radiusCone, heightCone), numPoints);
		this.radiusCylinder = radiusCylinder;
		this.radiusCone = radiusCone;
		this.heightCone = heightCone;
		this.heightCylinder = heightCylinder;
		vertexArray = new VertexArray(generatedData.vertexData);
		drawList = generatedData.drawList;		
	}
	
	public void bindData(ColorShaderProgram colorProgram) {
		vertexArray.setVertexAttribPointer(0, colorProgram.getPositionAttributeLocation(), POSITION_COMPONENT,
				4*(POSITION_COMPONENT+1));
		vertexArray.setVertexAttribPointer(POSITION_COMPONENT, colorProgram.getColorAttributeLocation(), 1,
				4*(POSITION_COMPONENT+1));
	}
	
	public void draw() {
		for (DrawCommand i: drawList) {
			i.draw();
		}
	}
	
}