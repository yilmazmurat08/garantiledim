"""Garantiledim tanıtım videosu için özgün, fragman tarzı müzik (46 sn, 120 BPM). Tamamen sentezlenir."""
import sys
import wave

import numpy as np

SR = 44100
DUR = 46.0
N = int(SR * DUR)
BPM = 120
BEAT = 60 / BPM          # 0.5 sn
BAR = 4 * BEAT           # 2 sn
rng = np.random.default_rng(42)

def t_arr(d):
    return np.arange(int(d * SR)) / SR

def place(buf, sig, at, gain=1.0):
    i = int(at * SR)
    if i >= len(buf):
        return
    j = min(len(buf), i + len(sig))
    buf[i:j] += sig[: j - i] * gain

def fft_filter(x, lo=None, hi=None, slope=2.0):
    """Statik yumuşak alçak/yüksek geçiren filtre (frekans alanında)."""
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    g = np.ones_like(f)
    if hi:
        g *= 1 / (1 + (f / hi) ** (2 * slope))
    if lo:
        g *= 1 / (1 + (lo / np.maximum(f, 1e-3)) ** (2 * slope))
    return np.fft.irfft(X * g, len(x))

def saw(freq, t, harmonics=24, bright=1.0):
    out = np.zeros_like(t)
    for k in range(1, harmonics + 1):
        if freq * k > 16000:
            break
        out += np.sin(2 * np.pi * freq * k * t) / k * np.exp(-(k - 1) * (0.18 / bright))
    return out

def env_adsr(n, a, d, s, r, total):
    t = np.arange(n) / SR
    e = np.where(t < a, t / max(a, 1e-4), 1.0)
    e = np.where((t >= a) & (t < a + d), 1 - (1 - s) * (t - a) / max(d, 1e-4), e)
    e = np.where(t >= a + d, s, e)
    e = np.where(t > total - r, e * np.clip((total - t) / max(r, 1e-4), 0, 1), e)
    return e

def note(n):
    return 440.0 * 2 ** ((n - 69) / 12)

# ---- enstrümanlar ----
def kick():
    t = t_arr(0.45)
    f = 45 + 110 * np.exp(-t * 28)
    ph = 2 * np.pi * np.cumsum(f) / SR
    return np.sin(ph) * np.exp(-t * 7) * 1.0 + np.exp(-t * 300) * rng.standard_normal(len(t)) * 0.15

def snare():
    t = t_arr(0.3)
    n = fft_filter(rng.standard_normal(len(t)), lo=1500, hi=9000) * np.exp(-t * 16)
    tone = np.sin(2 * np.pi * 190 * t) * np.exp(-t * 25)
    return n * 0.7 + tone * 0.5

def hat(open_=False):
    t = t_arr(0.25 if open_ else 0.07)
    return fft_filter(rng.standard_normal(len(t)), lo=7000) * np.exp(-t * (14 if open_ else 60)) * 0.35

def impact(d=3.5):
    t = t_arr(d)
    f = 30 + 60 * np.exp(-t * 6)
    boom = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 1.3)
    crack = fft_filter(rng.standard_normal(len(t)), lo=200, hi=6000) * np.exp(-t * 9) * 0.6
    return boom * 1.1 + crack

def braam(root_midi, d=3.0):
    t = t_arr(d)
    sig = np.zeros_like(t)
    for m, gain in [(root_midi - 12, 1.0), (root_midi, 0.8), (root_midi + 7, 0.6), (root_midi + 12, 0.35)]:
        for det in (-0.08, 0.0, 0.08):
            sig += saw(note(m + det), t, harmonics=30, bright=0.6) * gain
    e = np.minimum(t / 0.04, 1) * np.exp(-t * 0.9)
    return fft_filter(sig * e, hi=1400) * 0.22

def riser(d):
    t = t_arr(d)
    noise = rng.standard_normal(len(t))
    out = np.zeros_like(t)
    blocks = 40
    L = len(t) // blocks
    for b in range(blocks):
        seg = noise[b * L:(b + 1) * L]
        c = 400 * (20 ** (b / blocks))
        out[b * L:(b + 1) * L] = fft_filter(seg, lo=c * 0.5, hi=c * 2)
    sweep = np.sin(2 * np.pi * np.cumsum(200 * (8 ** (t / d))) / SR) * 0.25
    return (out * 0.8 + sweep) * (t / d) ** 2.2

def tick():
    t = t_arr(0.03)
    return np.sin(2 * np.pi * 2400 * t) * np.exp(-t * 180) * 0.5

def pluck(freq, d=0.35):
    t = t_arr(d)
    s = np.sin(2 * np.pi * freq * t) + 0.4 * np.sin(4 * np.pi * freq * t) + 0.15 * np.sin(6 * np.pi * freq * t)
    return s * np.exp(-t * 11) * 0.22

# ---- akor dizisi: Am – F – C – G (bar başına bir akor) ----
CH = [(57, [57, 60, 64]), (53, [53, 57, 60]), (48, [48, 52, 55]), (55, [55, 59, 62])]

drums = np.zeros(N); bass = np.zeros(N); pad = np.zeros(N); arp = np.zeros(N); fx = np.zeros(N); kick_env = np.zeros(N)

# Pad: tüm parça boyunca, intro ve outroda daha belirgin
for bar in range(int(DUR / BAR) + 1):
    start = bar * BAR
    if start >= DUR:
        break
    root, tones = CH[bar % 4]
    d = BAR + 0.3
    t = t_arr(d)
    s = np.zeros_like(t)
    for m in tones:
        for det in (-0.06, 0.06):
            s += saw(note(m + det), t, harmonics=14, bright=0.5)
    e = env_adsr(len(t), 0.4, 0.3, 0.8, 0.5, d)
    lvl = 0.10 if (start < 8 or start >= 40) else 0.06
    place(pad, s * e * lvl, start)

pad = fft_filter(pad, hi=2200)

# Intro (0–4): gerilim yükselişi ve logo vuruşu
place(fx, riser(1.0), 0.0, 0.35)
place(fx, impact(3.0), 1.0, 0.8)
place(fx, riser(1.4), 2.6, 0.6)
# Kanca (4–8): iki "braam" + saat tikleri + yükseliş
place(fx, impact(2.5), 4.0, 0.9); place(fx, braam(45), 4.0, 1.0)
place(fx, impact(2.5), 6.0, 0.9); place(fx, braam(41), 6.0, 1.0)
for k in range(8):
    place(fx, tick(), 4.0 + k * 0.5, 1.0)
place(fx, riser(1.6), 6.4, 0.7)

# Ana ritim (8–40)
for b in range(int((40 - 8) / BEAT)):
    at = 8 + b * BEAT
    place(drums, kick(), at, 0.9)
    place(kick_env, np.exp(-t_arr(0.3) * 10), at)
    if b % 2 == 1:
        place(drums, snare(), at, 0.55)
    place(drums, hat(), at + BEAT / 2, 0.8)
    place(drums, hat(), at, 0.4)
    if at >= 32:          # Premium bölümünde enerji artar
        place(drums, hat(True), at + BEAT / 2, 0.35)
# Bas: sekizlik notalar
for e8 in range(int((40 - 8) / (BEAT / 2))):
    at = 8 + e8 * BEAT / 2
    bar = int(at / BAR)
    root = CH[bar % 4][0] - 24
    t = t_arr(BEAT / 2)
    s = saw(note(root), t, harmonics=12, bright=0.5) * np.exp(-t * 4)
    place(bass, s * (0.22 if e8 % 2 == 0 else 0.16), at)
bass = fft_filter(bass, hi=700)
# Arpej: on altılıklar, 14 saniyeden sonra
pattern = [0, 1, 2, 1, 2, 0, 1, 2]
for s16 in range(int((40 - 14) / (BEAT / 4))):
    at = 14 + s16 * BEAT / 4
    bar = int(at / BAR)
    tones = CH[bar % 4][1]
    m = tones[pattern[s16 % len(pattern)]] + 12
    place(arp, pluck(note(m)), at, 0.9 if at >= 26 else 0.65)

# Sahne geçiş vuruşları
for at in (8, 14, 20, 26, 32, 36):
    place(fx, impact(2.0), at, 0.55)
for at in (13.0, 19.0, 25.0, 31.0, 35.0, 39.0):
    place(fx, riser(1.0), at, 0.35)

# Final (40–46): büyük vuruş + braam + kuyruk
place(fx, impact(5.0), 40.0, 1.0)
place(fx, braam(45, 5.5), 40.0, 1.1)

# Yan zincir benzeri pompalama
duck = 1 - 0.45 * np.clip(kick_env, 0, 1)
mix = drums * 0.9 + bass * duck + pad * duck * 1.2 + arp * duck + fx * 0.8

# Yankı (üstel gürültü IR ile FFT konvolüsyonu)
ir_t = t_arr(2.4)
ir = rng.standard_normal(len(ir_t)) * np.exp(-ir_t * 2.6)
ir = fft_filter(ir, hi=5000)
send = arp * 0.6 + fx * 0.35 + pad * 0.4
L = len(send) + len(ir)
wet = np.fft.irfft(np.fft.rfft(send, L) * np.fft.rfft(ir, L), L)[: len(send)]
wet /= np.max(np.abs(wet)) + 1e-9
mix = mix + wet * 0.18 * np.max(np.abs(mix))

# Stereo genişlik: küçük gecikmeli kopya
delay = int(0.012 * SR)
left = mix
right = np.concatenate([np.zeros(delay), mix[:-delay]]) * 0.92 + mix * 0.08

# Master: yumuşak kırpma, normalleştirme, son kararma
st = np.stack([left, right], axis=1)
st = np.tanh(st / (np.max(np.abs(st)) * 0.55))
st /= np.max(np.abs(st))
st *= 10 ** (-1 / 20)
fade = np.ones(N); fo = int(1.2 * SR); fade[-fo:] = np.linspace(1, 0, fo) ** 1.5
fi = int(0.05 * SR); fade[:fi] = np.linspace(0, 1, fi)
st *= fade[:, None]

out = sys.argv[1]
with wave.open(out, 'wb') as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes((st * 32767).astype(np.int16).tobytes())
print('yazıldı', out, f'{DUR} sn')
