uniform mat4 u_MVPMatrix[24]; 
attribute float a_MVPMatrixIndex;
attribute vec4 a_Position;
attribute vec2 a_TexCoordinate;
varying vec2 v_TexCoordinate;

void main() {
	int mvpMatrixIndex = int(a_MVPMatrixIndex);
	v_TexCoordinate = a_TexCoordinate; 
	gl_Position = u_MVPMatrix[mvpMatrixIndex] * a_Position; 
	gl_PointSize = 10.0;
}