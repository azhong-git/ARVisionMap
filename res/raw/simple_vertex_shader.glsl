uniform mat4 u_Matrix;
attribute vec4 a_Position;
attribute float a_Color;
varying float v_Color;

void main() {
	gl_Position = u_Matrix * a_Position;
	gl_PointSize = 10.0;
	v_Color = a_Color;
}