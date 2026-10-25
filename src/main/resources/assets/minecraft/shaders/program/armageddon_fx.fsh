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
//  - Cutscene film look: wide bloom, anamorphic streaks, edge fringe, filmic roll-off, split-tone,
//    lens dirt near the key light, vignette, film grain, fades to black
//  - Impact frames: ink on paper with halftone shading and speed lines

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
uniform float Impact;
uniform float ImpactMode;
uniform float Cine;
uniform float CineFade;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float luma(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

// Soft-knee threshold: only light brighter than lo contributes, fading in over 0.4.
vec3 bright(vec3 s, float lo) {
    return s * smoothstep(lo, lo + 0.4, luma(s));
}

// Filmic tone curve (ACES fit): rolls bright values off instead of clipping them.
vec3 filmic(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
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
        col += col * Flash * 1.2;
        col = mix(col, fc, clamp(Flash * 0.7 + lum * Flash * 0.15, 0.0, 0.5));
    }

    // --- Cutscene film look ---
    if (Cine > 0.0) {
        // Wide soft bloom: a golden-angle spiral of taps with gaussian weights.
        vec3 bloom = vec3(0.0);
        float wsum = 0.0;
        for (int i = 0; i < 24; i++) {
            float fi = float(i) + 0.5;
            float r = sqrt(fi / 24.0);
            float a = fi * 2.39996;
            vec2 o = vec2(cos(a), sin(a)) * r * 26.0 * oneTexel;
            float wgt = exp(-r * r * 2.5);
            bloom += bright(texture(DiffuseSampler, texCoord + o).rgb, 0.55) * wgt;
            wsum += wgt;
        }
        bloom /= wsum;
        // Anamorphic streak: very bright lights smear sideways into a thin blue line.
        vec3 streak = vec3(0.0);
        for (int i = 1; i <= 8; i++) {
            float d = float(i * i) * 1.6 + 2.0;
            float wgt = exp(-float(i) * 0.38);
            vec2 o = vec2(d * oneTexel.x, 0.0);
            streak += (bright(texture(DiffuseSampler, texCoord + o).rgb, 0.78) + bright(texture(DiffuseSampler, texCoord - o).rgb, 0.78)) * wgt;
        }
        streak = vec3(luma(streak)) * vec3(0.45, 0.65, 1.0) * 0.16;
        // Lens fringe that grows towards the frame edges.
        vec3 g = col;
        vec2 fr = center * dot(center, center) * 0.014;
        g.r = mix(g.r, texture(DiffuseSampler, uv + fr).r, 0.8);
        g.b = mix(g.b, texture(DiffuseSampler, uv - fr).b, 0.8);
        g += bloom * 0.85 + streak;
        // Filmic roll-off, a touch more saturation, teal shadows and warm highlights.
        g = mix(g, filmic(g * 1.1), 0.55);
        float lum = luma(g);
        g = mix(vec3(lum), g, 1.18);
        g *= mix(vec3(0.88, 0.98, 1.12), vec3(1.08, 1.0, 0.88), smoothstep(0.05, 0.75, lum));
        // Lens dirt catching the key light.
        if (GodStrength > 0.0) {
            vec2 dl = texCoord - vec2(GodX, GodY);
            dl.x *= aspect;
            float nearK = exp(-length(dl) * 2.2);
            vec2 dp = texCoord * vec2(aspect, 1.0);
            float dirt = smoothstep(0.62, 0.9, vnoise(dp * 9.0)) * 0.6 + smoothstep(0.75, 0.95, vnoise(dp * 23.0 + 7.0)) * 0.4;
            g += dirt * nearK * GodStrength * vec3(GodR, GodG, GodB) * 0.35;
        }
        float vig = 1.0 - smoothstep(0.35, 1.15, length(center * vec2(aspect, 1.0)));
        g *= mix(1.0, vig, 0.5);
        // Two-octave animated grain, stronger in the shadows like real film.
        vec2 gp = texCoord * InSize / 1.6;
        float frame = floor(GTime * 24.0);
        float grain = (hash(floor(gp) + frame * 1.37) + hash(floor(gp * 0.5) + frame * 2.11)) * 0.5 - 0.5;
        g += grain * 0.05 * (1.0 - lum * 0.6);
        col = mix(col, max(g, 0.0), Cine);
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
    // --- Anime impact frame: ink on paper, halftone shading, speed lines ---
    if (Impact > 0.5) {
        float l0 = luma(col);
        float lx1 = luma(texture(DiffuseSampler, texCoord + vec2(oneTexel.x * 2.0, 0.0)).rgb);
        float lx2 = luma(texture(DiffuseSampler, texCoord - vec2(oneTexel.x * 2.0, 0.0)).rgb);
        float ly1 = luma(texture(DiffuseSampler, texCoord + vec2(0.0, oneTexel.y * 2.0)).rgb);
        float ly2 = luma(texture(DiffuseSampler, texCoord - vec2(0.0, oneTexel.y * 2.0)).rgb);
        float edge = smoothstep(0.1, 0.28, abs(lx1 - lx2) + abs(ly1 - ly2));
        vec2 pp = texCoord * InSize;
        float fib = vnoise(pp * vec2(0.02, 0.35)) * 0.5 + vnoise(pp * 0.08) * 0.5;
        vec3 paper = vec3(0.95, 0.92, 0.85) * (0.9 + 0.1 * fib);
        vec3 inkc = vec3(0.07, 0.06, 0.09);
        // Rotated halftone screen: dark areas solid ink, midtones dots, highlights bare paper.
        vec2 hp = mat2(0.707, -0.707, 0.707, 0.707) * pp / 5.0;
        float dotd = length(fract(hp) - 0.5);
        float tone = clamp((l0 - 0.18) / 0.4, 0.0, 1.0);
        float r = (1.0 - tone) * 0.72;
        float shade = 1.0 - smoothstep(r - 0.05, r + 0.05, dotd);
        float inkAmt = max(shade, edge);
        vec3 ink;
        if (ImpactMode < 0.5) {
            ink = mix(paper, inkc, inkAmt);
        } else if (ImpactMode < 1.5) {
            ink = mix(inkc, paper, inkAmt);
        } else if (ImpactMode < 2.5) {
            vec3 red = vec3(0.82, 0.06, 0.08) * (0.9 + 0.1 * fib);
            ink = mix(mix(red, inkc, shade), paper, edge);
        } else {
            ink = mix(paper * 0.94, inkc, edge * 0.85);
        }
        // Radial speed lines towards the frame edges.
        float ang = atan(center.y, center.x * aspect);
        float lines = step(0.9, hash(vec2(floor(ang * 60.0), floor(GTime * 12.0))))
                * smoothstep(0.25, 0.7, length(center * vec2(aspect, 1.0)));
        ink = mix(ink, (ImpactMode > 0.5 && ImpactMode < 1.5) ? paper : inkc, lines * 0.85);
        col = ink;
    }
    if (Whiteout > 0.0) {
        col = mix(col, vec3(1.0), min(Whiteout, 0.5));
    }
    if (Blackout > 0.0) {
        float vig = smoothstep(0.2, 0.9, length(center * vec2(aspect, 1.0)));
        col = mix(col, vec3(0.0), clamp(Blackout * (0.75 + 0.25 * vig), 0.0, 1.0));
    }

    col *= 1.0 - clamp(CineFade, 0.0, 1.0);
    fragColor = vec4(col, 1.0);
}
