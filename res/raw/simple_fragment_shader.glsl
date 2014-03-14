precision mediump float;

uniform vec4 u_Color;
varying float v_Color;

void main() {
	gl_FragColor = vec4(u_Color.rgb*v_Color, u_Color.a);
}