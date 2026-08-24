// =========== Author ===========
// resource pack link: https://mapverse.net/map/find-the-tapes-minecraft-horror-map

#version 150

uniform float GameTime;
uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;
out vec4 fragColor;

// ============ VHS CONFIGURATION ============
// -- Optics / CRT --
uniform float BarrelAmount;   // screen curvature (cat eye), 0 = flat
uniform float ChromaAberration;  // base RGB separation (softened)
uniform float ChromaEdge;  // aberration boost toward the EDGES (CRT)
uniform float ChromaSmear;  // horizontal color smear (VHS chroma delay)

// -- Tracking band (the scrolling glitch, cassette signature) --
uniform float TrackSpeed;  // scroll speed (0 = fixed band)
uniform float TrackWidth;  // band width
uniform float TrackJitter;  // horizontal jitter inside the band
uniform float TrackBright;  // brightening inside the band (0 = off, avoids the white line)

// -- Signal instability --
uniform float FlickerAmount;  // brightness micro-variation (0 = off)

// -- Image texture --
uniform float ScanlineStrength;  // scanlines
uniform float GrainStrength;  // video grain
uniform float VignetteStrength;  // edge darkening

// -- Tape wobble & horizontal noise ("old tape" effects, 0 = off) --
uniform float OffsetIntensity;
uniform float NoiseIntensity;  // (default: 0.0016)

// -- Depth blur (blur with distance) --
uniform float BlurNear;
uniform float BlurFar;
uniform float BlurRadius;
uniform float BlurSamples;
uniform float MaxColorBlur;
// ===========================================

const float range = 0.05;
const float PI = 3.14159265;

float rand(vec2 co) {
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

vec2 barrelDistort(vec2 coord, float amount) {
    vec2 centered = coord * 2.0 - 1.0;
    float r2 = dot(centered, centered);
    centered *= 1.0 + amount * r2;
    return centered * 0.5 + 0.5;
}

float verticalBar(float pos, float uvY, float offset) {
    float x = smoothstep(pos - range, pos, uvY) * offset;
    x -= smoothstep(pos, pos + range, uvY) * offset;
    return x;
}

// Scanlines (with a slight vertical scroll = analog roll)
float scanlines(vec2 uv, float t) {
    float line = sin((uv.y + t * 0.0002) * 600.0) * 0.5 + 0.5;
    return mix(1.0, 0.92, line * ScanlineStrength);
}

float filmGrain(vec2 uv, float time) {
    return (rand(uv + vec2(time * 0.1)) - 0.5) * GrainStrength;
}

float vignette(vec2 uv) {
    vec2 pos = (uv - 0.5) * 2.0;
    float dist = length(pos);
    return smoothstep(1.4, 0.6, dist) * VignetteStrength + (1.0 - VignetteStrength);
}

void main() {
    float time = GameTime * 1200.0 * 6.2831853;
    float gt   = GameTime * 1200.0;            // "slow" time for the animated effects

    // Screen distortion (CRT)
    vec2 uv = barrelDistort(texCoord, BarrelAmount);

    // ---- VHS tracking band: scrolls vertically + horizontal jitter ----
    float bandPos  = fract(gt * TrackSpeed * 0.01);
    float bandDist = abs(fract(uv.y - bandPos + 0.5) - 0.5);     // distance to the band (wrap)
    float band     = smoothstep(TrackWidth, 0.0, bandDist);     // 1 inside the band, 0 outside
    uv.x += band * (rand(vec2(floor(uv.y * 140.0), floor(gt))) - 0.5) * TrackJitter;

    // Black corners off-screen
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }

    // Vertical bars (tape wobble, off by default)
    for (int i = 0; i < 6; i++) {
        float fi = float(i) * 0.1313;
        float d = mod(GameTime * 1200.0 * fi * 0.7, 1.7);
        float o = sin(1.0 - tan(time * 0.05 * 0.24 * fi));
        o *= OffsetIntensity;
        uv.x += verticalBar(d, uv.y, o);
    }

    // Horizontal noise (tracking noise)
    float noiseQuality = 100.0;
    float uvYq = uv.y * noiseQuality;
    uvYq = float(int(uvYq)) * (1.0 / noiseQuality);
    float lineNoise = rand(vec2(time * 0.00001, uvYq));
    uv.x += lineNoise * NoiseIntensity;

    uv = clamp(uv, 0.001, 0.999);

    // ---- Depth blur (blur with distance) ----
    float depth = texture(DepthSampler, uv).r;
    float blurAmount = smoothstep(BlurNear, BlurFar, depth) * BlurRadius;

    vec3 color;
    if (blurAmount < 0.0001) {
        color = texture(DiffuseSampler, uv).rgb;
    } else {
        color = vec3(0.0);
        for (int i = 0; i < BlurSamples; i++) {
            float angle = float(i) * (2.0 * PI / float(BlurSamples));
            vec2 offset = vec2(cos(angle), sin(angle)) * blurAmount;
            color += texture(DiffuseSampler, clamp(uv + offset, 0.001, 0.999)).rgb;
        }
        color /= float(BlurSamples);
        color = mix(texture(DiffuseSampler, uv).rgb, color, MaxColorBlur);
    }

    // ---- RADIAL chromatic aberration (boosted at the edges) + horizontal smear ----
    if (ChromaAberration > 0.0) {
        vec2  dir  = texCoord - 0.5;
        float amt  = ChromaAberration * (0.35 + dot(dir, dir) * ChromaEdge);
        vec2  off  = dir * amt * 0.02 + vec2(ChromaSmear, 0.0);
        color.r = texture(DiffuseSampler, clamp(uv + off, 0.001, 0.999)).r;
        color.b = texture(DiffuseSampler, clamp(uv - off, 0.001, 0.999)).b;
    }

    // ---- VHS image texture ----
    color *= scanlines(uv, gt);
    color += vec3(filmGrain(uv, time));

    // Signal instability (brightness micro-flicker)
    color *= 1.0 + FlickerAmount * (rand(vec2(floor(gt * 2.0), 7.3)) - 0.5);

    // Brightening + slight color wash in the tracking band
    color += band * TrackBright;
    color = mix(color, vec3(dot(color, vec3(0.299, 0.587, 0.114))), band * 0.08);

    // Vignette
    color *= vignette(texCoord);

    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}