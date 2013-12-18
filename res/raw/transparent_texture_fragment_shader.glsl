precision mediump float;

uniform sampler2D u_TextureUnit;
varying vec2 v_TextureCoord;

void main() {
	vec4 color = texture2D(u_TextureUnit, v_TextureCoord).rgba;
	color = (color.a > 0.99) ? vec4(color.rgb, 0.2) : color;
	gl_FragColor = color;
}