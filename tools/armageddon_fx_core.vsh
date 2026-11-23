#version 150

// The particle effects' vertex shader: camera-relative positions, so the distance to the eye is just their length.

in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

out float vertexDistance;
out vec2 texCoord0;
out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexDistance = FogShape == 0 ? length(Position) : max(length(Position.xz), abs(Position.y));
    texCoord0 = UV0;
    vertexColor = Color;
}
