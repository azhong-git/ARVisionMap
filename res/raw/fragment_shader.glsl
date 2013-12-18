uniform sampler2D u_Texture;
precision mediump float;

uniform vec4 u_Color;
varying vec2 v_TexCoord;

void main() {
	gl_FragColor = texture2D(u_Texture, v_TexCoord).w * u_Color;
}