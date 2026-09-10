#version 150

uniform sampler2D DiffuseSampler;
uniform float BoundaryPhase;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 source = texture(DiffuseSampler, texCoord);
    float luma = dot(source.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 grayscale = vec3(luma);
    vec3 desaturated = mix(source.rgb, grayscale, BoundaryPhase);
    float contrast = 1.0 + 0.80 * BoundaryPhase;
    vec3 result = (desaturated - 0.5) * contrast + 0.5;
    fragColor = vec4(clamp(result, 0.0, 1.0), source.a);
}
