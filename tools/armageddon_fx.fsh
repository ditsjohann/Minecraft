#version 150

// Armageddon Arsenal post-processing pass.
//  - Gravitational lensing around the most prominent on-screen black hole
//  - Screen-space shockwave ripple when a blast front passes the camera
//  - Heat haze + chromatic aberration after detonations
//  - Volumetric god rays streaming from fireballs / stars / light pillars
//  - Rainbow hue cycling for bass weapons
//  - Radiation-sickness vision: green cast, film grain, vignette, scanline glitches
//  - Nuclear flash: over-exposure blooming into a white-out
//  - World-event grading: desaturation, white-out, black-out, signal static, inversion

uniform sampler2D DiffuseSampler;

uniform vec2 InSize;
uniform float Flash;
uniform float FlashR;
uniform float FlashG;
uniform float FlashB;
uniform float Distortion;
uniform float Radiation;
uniform float GTime;
uniform float LensX;
uniform float LensY;
uniform float LensR;
uniform float LensStrength;
uniform float GodX;
uniform float GodY;
uniform float GodStrength;
uniform float GodR;
uniform float GodG;
uniform float GodB;
uniform float Ripple;
uniform float Hue;
uniform float Desaturate;
uniform float Whiteout;
uniform float Blackout;
uniform float Static;
uniform float Invert;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

vec3 hueShift(vec3 color, float shift) {
    const vec3 k = vec3(0.57735);
    float c = cos(shift);
    return color * c + cross(k, color) * sin(shift) + k * dot(k, color) * (1.0 - c);
}

void main() {
    vec2 uv = texCoord;
    float aspect = InSize.x / InSize.y;
    float horizon = 0.0;
    float ring = 0.0;

    // --- Gravitational lensing (weak-field Schwarzschild approximation in screen space) ---
    if (LensStrength > 0.0) {
        vec2 d = uv - vec2(LensX, LensY);
        d.x *= aspect;
        float r = length(d);
        float rs = max(LensR, 0.0005);
        vec2 dir = r > 0.0 ? d / r : vec2(0.0);
        float bend = rs * rs * 2.2 / max(r, rs * 0.4);
        float fade = smoothstep(rs * 10.0, rs * 3.0, r);
        vec2 offs = dir * min(bend, r * 0.95) * fade * LensStrength;
        offs.x /= aspect;
        uv -= offs;
        horizon = smoothstep(rs * 1.08, rs * 0.92, r) * LensStrength;
        ring = exp(-pow((r - rs * 1.5) / (rs * 0.18), 2.0)) * 0.35 * LensStrength;
    }

    // --- Shockwave ripple: an expanding refractive ring from the screen centre ---
    if (Ripple < 1.0) {
        vec2 d = uv - 0.5;
        d.x *= aspect;
        float r = length(d);
        float front = Ripple * 1.4;
        float w = 0.08;
        float k = exp(-pow((r - front) / w, 2.0)) * (1.0 - Ripple);
        vec2 dir = r > 0.0 ? d / r : vec2(0.0);
        dir.x /= aspect;
        uv += dir * k * 0.045;
    }

    // --- Heat haze / shock distortion ---
    if (Distortion > 0.0) {
        float w = sin(uv.y * 90.0 + GTime * 9.0) * cos(uv.x * 70.0 - GTime * 7.0);
        uv += vec2(w, -w) * 0.0028 * Distortion;
    }

    // --- Chromatic aberration ---
    vec2 center = uv - 0.5;
    float ca = Distortion * 0.014 + Radiation * 0.005 + LensStrength * 0.002 + Hue * 0.012 + (1.0 - Ripple) * 0.01;
    vec3 col;
    col.r = texture(DiffuseSampler, uv + center * ca + oneTexel * 0.0).r;
    col.g = texture(DiffuseSampler, uv).g;
    col.b = texture(DiffuseSampler, uv - center * ca).b;

    col *= (1.0 - horizon);
    col += vec3(1.0, 0.85, 0.6) * ring;

    // --- God rays: radial march towards the light, accumulating bright pixels ---
    if (GodStrength > 0.0) {
        vec2 lightUv = vec2(GodX, GodY);
        vec2 delta = (texCoord - lightUv) / 28.0;
        vec2 suv = texCoord;
        float decay = 1.0;
        vec3 rays = vec3(0.0);
        for (int i = 0; i < 28; i++) {
            suv -= delta;
            vec3 s = texture(DiffuseSampler, clamp(suv, 0.0, 1.0)).rgb;
            float lum = dot(s, vec3(0.299, 0.587, 0.114));
            rays += s * smoothstep(0.55, 1.0, lum) * decay;
            decay *= 0.94;
        }
        rays /= 28.0;
        vec2 dl = texCoord - lightUv;
        dl.x *= aspect;
        float glow = exp(-length(dl) * 3.5);
        col += (rays * 2.6 + glow * 0.35) * vec3(GodR, GodG, GodB) * GodStrength;
    }

    // --- Rainbow bass hue cycling ---
    if (Hue > 0.0) {
        float shift = (GTime * 4.0 + length(center) * 6.0) * Hue;
        col = mix(col, hueShift(col, shift), Hue);
        col *= 1.0 + 0.25 * Hue * sin(GTime * 30.0);
        float vig = smoothstep(0.9, 0.35, length(center * vec2(aspect, 1.0)));
        col *= mix(1.0, vig, Hue * 0.5);
    }

    // --- Radiation sickness vision ---
    if (Radiation > 0.0) {
        float lum = dot(col, vec3(0.299, 0.587, 0.114));
        vec3 tint = mix(col, vec3(lum * 0.7, lum * 1.18, lum * 0.55), Radiation * 0.6);
        float grain = hash(floor(texCoord * InSize / 2.0) + floor(GTime * 24.0)) - 0.5;
        tint += grain * 0.24 * Radiation;
        float vig = smoothstep(0.9, 0.25, length(center * vec2(aspect, 1.0)));
        tint *= mix(1.0, vig, Radiation * 0.85);
        float glitch = step(0.985, hash(vec2(floor(GTime * 12.0), floor(texCoord.y * 60.0))));
        tint += vec3(0.0, 0.1, 0.02) * Radiation * glitch;
        col = tint;
    }

    // --- Nuclear flash ---
    if (Flash > 0.0) {
        vec3 fc = vec3(FlashR, FlashG, FlashB);
        float lum = dot(col, vec3(0.333));
        col += col * Flash * 2.5;
        col = mix(col, fc, clamp(Flash * 1.15 - 0.1 + lum * Flash * 0.3, 0.0, 1.0));
    }

    // --- World-event colour grading ---
    if (Desaturate > 0.0) {
        float lum = dot(col, vec3(0.299, 0.587, 0.114));
        col = mix(col, vec3(lum), Desaturate);
    }
    if (Static > 0.0) {
        float band = step(0.93, hash(vec2(floor(GTime * 20.0), floor(texCoord.y * 40.0))));
        vec2 suv = texCoord + vec2((hash(vec2(GTime, texCoord.y)) - 0.5) * 0.06 * band * Static, 0.0);
        vec3 shifted = texture(DiffuseSampler, suv).rgb;
        float n = hash(floor(texCoord * InSize / 1.5) + floor(GTime * 30.0));
        col = mix(col, mix(shifted, vec3(n), 0.45), Static);
    }
    if (Invert > 0.0) {
        col = mix(col, vec3(1.0) - col, Invert);
    }
    if (Whiteout > 0.0) {
        col = mix(col, vec3(1.0), Whiteout);
    }
    if (Blackout > 0.0) {
        float vig = smoothstep(0.2, 0.9, length(center * vec2(aspect, 1.0)));
        col = mix(col, vec3(0.0), clamp(Blackout * (0.75 + 0.25 * vig), 0.0, 1.0));
    }

    fragColor = vec4(col, 1.0);
}
