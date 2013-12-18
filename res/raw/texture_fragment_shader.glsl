precision mediump float;

uniform sampler2D u_TextureUnit;
varying vec2 v_TextureCoord;

void main() {
	vec3 color = texture2D(u_TextureUnit, v_TextureCoord).rgb;
	gl_FragColor = vec4(color, 0.3);
}