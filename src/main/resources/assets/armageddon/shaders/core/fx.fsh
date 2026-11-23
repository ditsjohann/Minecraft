#version 150

// The particle effects in one pass with premultiplied alpha (blend ONE, ONE_MINUS_SRC_ALPHA): smoke carries its
// coverage in alpha and hides what is behind it by that much; glow has alpha 0 and simply adds its light. So smoke
// and glow can be sorted back to front together and each veils or lights the other correctly. There is no alpha
// cut-off, so soft edges fade all the way out, and distance fog is applied like the world's (premultiplied: fogged
// smoke turns toward the fog colour, fogged glow fades away).

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a <= 0.0 && max(color.r, max(color.g, color.b)) <= 0.001) {
        discard;
    }
    float fog = vertexDistance <= FogStart ? 0.0 : (vertexDistance < FogEnd ? smoothstep(FogStart, FogEnd, vertexDistance) : 1.0);
    fog *= FogColor.a;
    fragColor = vec4(mix(color.rgb, FogColor.rgb * color.a, fog), color.a);
}
