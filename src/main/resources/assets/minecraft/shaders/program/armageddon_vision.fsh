#version 150

// Armageddon Arsenal "vision" pass: depth-aware screen effects drawn over the finished world (before the hand).
// Unlike the main post pass this one can see the depth buffer, so every mode knows where each pixel sits in the
// world and can work in world space:
//   1 LIDAR    - the world as a point cloud of laser returns, revealed by expanding scan pulses, targets in red
//   2 VOID     - a volumetric sphere of darkness: rays are absorbed by the length they travel inside it,
//                with a black event core and a violet rim, and violet ink edges on whatever is inside
//   3 ECLIPSE  - the sun is gone: distance fog to black, a starless sky, cold desaturated light, frost on the lens
//   4 SONAR    - green phosphor: echo rings roll out from the source, edges glow as they are pinged
//   5 PIXEL    - the world inside the radius drops to a low-res, dithered sixteen-colour palette
//   6 COMIC    - ink outlines, cel bands, halftone dots and paper, spreading out from the source
//   7 COMPOUND - seen through an insect's compound eye: a lattice of hexagonal lenses with an amber cast

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform mat4 InvViewProj;
uniform mat4 Targets;
uniform vec2 InSize;
uniform vec3 Origin;
uniform vec3 CamFrac;
uniform vec3 Color;
uniform float Mode;
uniform float Strength;
uniform float Time;
uniform float Radius;
uniform float Reveal;
uniform float Param;
uniform float P11;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

float hash3(vec3 p) {
    return fract(sin(dot(p, vec3(12.9898, 78.233, 37.719))) * 43758.5453);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

float luma(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

float depthAt(vec2 uv) {
    return texture(DepthSampler, uv).r;
}

// Camera-relative world position of the surface seen at uv.
vec3 relAt(vec2 uv) {
    vec4 w = InvViewProj * vec4(uv * 2.0 - 1.0, depthAt(uv) * 2.0 - 1.0, 1.0);
    return w.xyz / w.w;
}

// Direction of the view ray through uv.
vec3 rayAt(vec2 uv) {
    vec4 w = InvViewProj * vec4(uv * 2.0 - 1.0, 1.0, 1.0);
    return normalize(w.xyz / w.w);
}

// Surface normal from neighbouring positions, using the smaller difference on each axis so edges stay clean.
vec3 normalAt(vec2 uv, vec3 p) {
    vec3 px1 = relAt(uv + vec2(oneTexel.x, 0.0)) - p;
    vec3 px2 = p - relAt(uv - vec2(oneTexel.x, 0.0));
    vec3 py1 = relAt(uv + vec2(0.0, oneTexel.y)) - p;
    vec3 py2 = p - relAt(uv - vec2(0.0, oneTexel.y));
    vec3 dx = dot(px1, px1) < dot(px2, px2) ? px1 : px2;
    vec3 dy = dot(py1, py1) < dot(py2, py2) ? py1 : py2;
    vec3 n = cross(dx, dy);
    return dot(n, n) > 1.0e-12 ? normalize(n) : vec3(0.0, 1.0, 0.0);
}

// 0..1 silhouette / crease strength from depth discontinuities around uv.
float edgeAt(vec2 uv, vec3 p, float w) {
    float d0 = length(p);
    float e = 0.0;
    vec2 o = oneTexel * w;
    e = max(e, abs(length(relAt(uv + vec2(o.x, 0.0))) - d0));
    e = max(e, abs(length(relAt(uv - vec2(o.x, 0.0))) - d0));
    e = max(e, abs(length(relAt(uv + vec2(0.0, o.y))) - d0));
    e = max(e, abs(length(relAt(uv - vec2(0.0, o.y))) - d0));
    return smoothstep(0.04, 0.12, e / max(d0, 0.5));
}

// How strongly a living target (packed into the columns of Targets, w = 1 when used) marks point p.
float targetMark(vec3 p) {
    float m = 0.0;
    for (int i = 0; i < 4; i++) {
        vec4 t = Targets[i];
        if (t.w > 0.5) {
            vec3 d = p - t.xyz;
            d.y *= 0.6;
            m = max(m, 1.0 - smoothstep(0.9, 1.8, length(d)));
        }
    }
    return m;
}

// ------------------------------------------------------------------ LIDAR

vec3 lidar(vec2 uv, vec3 scene) {
    float aspect = InSize.x / InSize.y;
    vec3 col = vec3(0.0, 0.006, 0.012);
    // Faint CRT rows and a slow sweep bar.
    col += vec3(0.0, 0.012, 0.02) * (0.5 + 0.5 * sin(uv.y * InSize.y * 1.4));
    col += vec3(0.0, 0.05, 0.07) * exp(-pow((uv.y - fract(Time * 0.21)) * 30.0, 2.0));
    float depth = depthAt(uv);
    if (depth < 1.0) {
        vec3 p = relAt(uv);
        float dist = length(p);
        vec3 wp = p + CamFrac;
        vec3 n = abs(normalAt(uv, p));
        vec2 q = (n.x > n.y && n.x > n.z) ? wp.yz : ((n.y > n.z) ? wp.xz : wp.xy);
        float spacing = 0.5;
        vec2 cell = floor(q / spacing);
        float keep = step(0.18, hash(cell + 3.1));
        vec2 jit = vec2(hash(cell), hash(cell + 17.0)) * 0.6 + 0.2;
        vec2 f = q / spacing - cell - jit;
        float px = dist * 2.0 / (P11 * InSize.y) / spacing;
        float r = max(0.075, px * 1.15);
        float dotv = (1.0 - smoothstep(r * 0.55, r, length(f))) * keep;
        // Far away the points are smaller than a pixel: fade them into a flat dim return.
        dotv = mix(dotv, 0.32, smoothstep(0.22, 0.6, px));
        float od = distance(p, Origin);
        float revealed = 1.0 - smoothstep(Reveal - 4.0, Reveal, od);
        float k = clamp(od / max(Param, 1.0), 0.0, 1.0);
        vec3 pal = k < 0.5 ? mix(vec3(0.75, 1.0, 1.0), vec3(0.1, 0.7, 1.0), k * 2.0) : mix(vec3(0.1, 0.7, 1.0), vec3(0.85, 0.2, 1.0), k * 2.0 - 1.0);
        // Height bands, like a survey scan.
        float band = 0.82 + 0.18 * step(0.5, fract(wp.y * 0.25));
        vec3 c = pal * band * dotv * revealed * (0.6 + 0.4 * (1.0 - k));
        float front = exp(-pow((od - Radius) / 2.2, 2.0));
        c += vec3(0.8, 1.0, 1.0) * front * (0.25 + 0.9 * dotv);
        float t = targetMark(p);
        c = mix(c, vec3(1.0, 0.12, 0.08) * (0.35 + 1.2 * dotv), t * max(revealed, front));
        col += c;
    }
    // Range rings in the corner of the eye.
    vec2 cc = (uv - 0.5) * vec2(aspect, 1.0);
    float rr = length(cc);
    col += vec3(0.0, 0.25, 0.3) * (1.0 - smoothstep(0.0015, 0.003, abs(fract(rr * 4.0 - Time * 0.5) - 0.5) * 0.02)) * smoothstep(0.55, 0.9, rr) * 0.25;
    return col;
}

// ------------------------------------------------------------------ VOID

vec3 voidVision(vec2 uv, vec3 scene) {
    float depth = depthAt(uv);
    vec3 p = relAt(uv);
    vec3 dir = depth < 1.0 ? normalize(p) : rayAt(uv);
    float tmax = depth < 1.0 ? length(p) : 1.0e5;
    vec3 oc = -Origin;
    float b = dot(oc, dir);
    float c2 = dot(oc, oc);
    float closest = sqrt(max(c2 - b * b, 0.0));
    float absorb = 0.0;
    float rim = 0.0;
    float core = 0.0;
    float h = Radius * Radius - (c2 - b * b);
    if (h > 0.0) {
        float s = sqrt(h);
        float t0 = max(-b - s, 0.0);
        float t1 = min(-b + s, tmax);
        float len = max(t1 - t0, 0.0);
        absorb = 1.0 - exp(-len * 4.5 / max(Radius, 1.0));
    }
    if (-b > 0.0 || c2 < Radius * Radius) {
        rim = exp(-pow((closest - Radius) / max(Radius * 0.05, 0.4), 2.0));
        float wob = vnoise(vec2(atan(dir.z, dir.x) * 8.0, dir.y * 8.0) + Time * 0.6);
        rim *= 0.6 + 0.8 * wob;
        if (tmax > -b - sqrt(max(Param * Param - closest * closest, 0.0))) {
            core = 1.0 - smoothstep(Param * 0.97, Param, closest);
        }
    }
    vec3 col = scene;
    // Everything near the void loses its colour and light.
    float near = 0.0;
    if (depth < 1.0) {
        float od = distance(p, Origin);
        near = 1.0 - smoothstep(Radius, Radius * 2.2, od);
        float e = edgeAt(uv, p, 1.5);
        col = mix(col, vec3(luma(col)) * vec3(0.55, 0.45, 0.8), near * 0.85);
        col *= 1.0 - near * 0.55;
        col += Color * e * near * 0.9;
    }
    col *= 1.0 - absorb * 0.97;
    col += Color * rim * 0.9;
    // Smoky tendrils at the boundary, crawling.
    float smoke = vnoise(uv * vec2(InSize.x / InSize.y, 1.0) * 9.0 + vec2(Time * 0.3, -Time * 0.5));
    col = mix(col, vec3(0.0), absorb * smoke * 0.3);
    col = mix(col, vec3(0.0), core);
    col += Color * 0.6 * core * exp(-pow((closest - Param * 0.985) / max(Param * 0.03, 0.2), 2.0));
    return col;
}

// ------------------------------------------------------------------ ECLIPSE

vec3 eclipse(vec2 uv, vec3 scene) {
    float aspect = InSize.x / InSize.y;
    float depth = depthAt(uv);
    vec3 col;
    vec3 cold = vec3(luma(scene)) * vec3(0.55, 0.72, 1.0);
    if (depth < 1.0) {
        vec3 p = relAt(uv);
        float dist = length(p);
        float fog = 1.0 - exp(-dist * Param);
        col = mix(mix(scene, cold, 0.8) * 0.7, vec3(0.0, 0.004, 0.012), fog);
    } else {
        vec3 d = rayAt(uv);
        float st = step(0.9965, hash3(floor(d * 260.0)));
        col = vec3(0.0, 0.002, 0.008) + vec3(0.25, 0.3, 0.4) * st * Color.r;
    }
    // Frost creeping over the lens from the edges.
    vec2 cc = (uv - 0.5) * vec2(aspect, 1.0);
    float edge = smoothstep(0.55, 1.05, length(cc) + (vnoise(uv * 5.0) - 0.5) * 0.25);
    vec2 fp = uv * vec2(aspect, 1.0) * 18.0;
    float crystal = pow(1.0 - abs(vnoise(fp) - 0.5) * 2.0, 14.0) + pow(1.0 - abs(vnoise(fp * 2.3 + 4.0) - 0.5) * 2.0, 18.0) * 0.5;
    col = mix(col, vec3(0.72, 0.86, 1.0), clamp(edge * (0.35 + crystal * 0.6), 0.0, 0.85) * Color.g);
    return col;
}

// ------------------------------------------------------------------ SONAR

vec3 sonar(vec2 uv, vec3 scene) {
    vec3 green = vec3(0.2, 1.0, 0.45);
    vec3 col = vec3(0.0, 0.035, 0.018) + vec3(0.0, 0.02, 0.01) * hash(floor(uv * InSize / 2.0) + floor(Time * 20.0));
    float depth = depthAt(uv);
    if (depth < 1.0) {
        vec3 p = relAt(uv);
        float od = distance(p, Origin);
        float e = edgeAt(uv, p, 1.0);
        vec3 n = normalAt(uv, p);
        float facing = abs(dot(n, normalize(p - Origin)));
        float since = Radius - od;
        // Every surface the echo has passed keeps a fading afterglow; the next ring refreshes it.
        float glow = since > 0.0 ? exp(-mod(since, max(Param, 4.0)) * 0.08) : 0.0;
        float front = exp(-pow(mod(since + Param * 0.5, max(Param, 4.0)) - Param * 0.5, 2.0) / 3.0) * step(0.0, since + 2.0);
        col += green * (0.12 + 0.6 * e + 0.25 * facing) * glow;
        col += green * front * (0.4 + 0.6 * facing);
        float t = targetMark(p);
        col = mix(col, vec3(1.0, 0.75, 0.15) * (0.4 + glow), t * step(0.0, since));
        col += green * 0.05 * luma(scene);
    }
    float scan = 0.85 + 0.15 * sin(uv.y * InSize.y * 1.6);
    return col * scan;
}

// ------------------------------------------------------------------ PIXEL

float bayer4(vec2 p) {
    vec2 i = mod(floor(p), 4.0);
    int idx = int(i.x) + int(i.y) * 4;
    float m[16] = float[16](0.0, 8.0, 2.0, 10.0, 12.0, 4.0, 14.0, 6.0, 3.0, 11.0, 1.0, 9.0, 15.0, 7.0, 13.0, 5.0);
    return m[idx] / 16.0 - 0.5;
}

vec3 pixel(vec2 uv, vec3 scene) {
    float blk = max(1.0, floor(Param));
    vec2 cellIdx = floor(uv * InSize / blk);
    vec2 g = (cellIdx + 0.5) * blk / InSize;
    float depth = depthAt(g);
    bool inside = Radius > 4000.0;
    if (!inside) {
        if (depth < 1.0) {
            inside = distance(relAt(g), Origin) < Radius;
        } else {
            inside = Radius > 300.0;
        }
    }
    if (!inside) {
        return scene;
    }
    vec3 c = texture(DiffuseSampler, g).rgb;
    c = clamp(mix(vec3(luma(c)), c, 1.3), 0.0, 1.0);
    c = clamp(floor(c * 7.0 + 0.5 + bayer4(cellIdx) * 0.3) / 7.0, 0.0, 1.0);
    // A thin darker seam between the big pixels reads as a screen door.
    vec2 f = fract(uv * InSize / blk);
    float seam = step(f.x, 1.0 / blk) + step(f.y, 1.0 / blk);
    c *= 1.0 - 0.07 * min(seam, 1.0) * step(4.0, blk);
    return c;
}

// ------------------------------------------------------------------ COMIC

vec3 comic(vec2 uv, vec3 scene) {
    float depth = depthAt(uv);
    vec3 p = relAt(uv);
    float od = depth < 1.0 ? distance(p, Origin) : 1.0e4;
    float splat = (vnoise(uv * 14.0) - 0.5) * 6.0 + (vnoise(uv * 50.0) - 0.5) * 2.0;
    float inside = depth < 1.0 ? 1.0 - smoothstep(Radius - 1.0, Radius + 1.0, od + splat) : step(500.0, Radius);
    if (inside <= 0.0) {
        return scene;
    }
    float l = luma(scene);
    vec3 hue = scene / max(max(scene.r, max(scene.g, scene.b)), 0.04);
    float tone = l < 0.14 ? 0.25 : (l < 0.38 ? 0.62 : 0.95);
    vec3 c = mix(scene / max(l, 0.05) * 0.55, hue, 0.5) * tone;
    c = clamp(mix(vec3(luma(c)), c, 1.15), 0.0, 1.0);
    vec2 pp = uv * InSize;
    // Halftone in the mid band.
    vec2 hp = mat2(0.707, -0.707, 0.707, 0.707) * pp / 6.0;
    float dotd = length(fract(hp) - 0.5);
    float dr = clamp((0.62 - l) * 1.1, 0.0, 0.6);
    c *= 1.0 - 0.45 * (1.0 - smoothstep(dr - 0.05, dr + 0.05, dotd)) * step(0.16, l);
    float e = 0.0;
    if (depth < 1.0) {
        e = edgeAt(uv, p, 2.5);
        vec3 n = normalAt(uv, p);
        vec3 n2 = normalAt(uv + oneTexel * 2.0, relAt(uv + oneTexel * 2.0));
        e = max(e, smoothstep(0.5, 0.8, 1.0 - dot(n, n2)));
    } else {
        c = vec3(0.98, 0.93, 0.78);
    }
    vec3 paper = vec3(1.0, 0.97, 0.9) * (0.92 + 0.08 * vnoise(pp * vec2(0.03, 0.4)));
    c *= paper;
    c = mix(c, vec3(0.05, 0.04, 0.07), e);
    return mix(scene, c, inside);
}

// ------------------------------------------------------------------ COMPOUND EYE

vec3 compound(vec2 uv, vec3 scene) {
    float size = max(Param, 6.0);
    vec2 pp = uv * InSize / size;
    // Nearest centre on a hexagonal lattice.
    vec2 r = vec2(1.0, 1.7320508);
    vec2 h = r * 0.5;
    vec2 a = mod(pp, r) - h;
    vec2 b = mod(pp - h, r) - h;
    vec2 gv = dot(a, a) < dot(b, b) ? a : b;
    vec2 centre = (pp - gv) * size / InSize;
    float dist = length(gv);
    // Each facet is a small lens: it sees a slightly magnified, jittered patch of the scene.
    vec2 jitter = (vec2(hash(floor(centre * 400.0)), hash(floor(centre * 400.0) + 9.0)) - 0.5) * oneTexel * size * 0.6;
    vec3 c = texture(DiffuseSampler, clamp(centre + gv * size / InSize * 0.55 + jitter, 0.0, 1.0)).rgb;
    c *= mix(vec3(1.08, 0.92, 0.55), vec3(1.0), 0.35);
    c *= 1.0 - smoothstep(0.3, 0.5, dist) * 0.85;
    c += vec3(1.0, 0.9, 0.6) * exp(-pow((dist - 0.2) / 0.05, 2.0)) * 0.08;
    return c;
}

void main() {
    vec3 scene = texture(DiffuseSampler, texCoord).rgb;
    vec3 col = scene;
    if (Mode < 1.5) {
        col = lidar(texCoord, scene);
    } else if (Mode < 2.5) {
        col = voidVision(texCoord, scene);
    } else if (Mode < 3.5) {
        col = eclipse(texCoord, scene);
    } else if (Mode < 4.5) {
        col = sonar(texCoord, scene);
    } else if (Mode < 5.5) {
        col = pixel(texCoord, scene);
    } else if (Mode < 6.5) {
        col = comic(texCoord, scene);
    } else {
        col = compound(texCoord, scene);
    }
    fragColor = vec4(mix(scene, col, clamp(Strength, 0.0, 1.0)), 1.0);
}
