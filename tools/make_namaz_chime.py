"""Generates PixelQuest's prayer-reminder chime: three soft bell tones, made from sine waves here
(no recording), so the app owns it outright. 22.05 kHz, 16-bit mono WAV, about 2.6 seconds."""
import math
import struct
import sys
import wave

RATE = 22050
LENGTH = 2.6
# A gentle rising major arpeggio (E5, G#5, B5), each note starting 0.35 s after the last.
NOTES = [(659.25, 0.0), (830.61, 0.35), (987.77, 0.70)]


def bell(freq, t):
    # A bell: the fundamental with two soft partials, a quick attack and a long decay.
    if t < 0:
        return 0.0
    attack = min(1.0, t / 0.012)
    decay = math.exp(-t * 2.4)
    tone = (math.sin(2 * math.pi * freq * t)
            + 0.30 * math.sin(2 * math.pi * freq * 2.0 * t) * math.exp(-t * 3.0)
            + 0.12 * math.sin(2 * math.pi * freq * 3.01 * t) * math.exp(-t * 5.0))
    return attack * decay * tone


frames = bytearray()
count = int(RATE * LENGTH)
for i in range(count):
    t = i / RATE
    sample = sum(bell(freq, t - start) for freq, start in NOTES) * 0.22
    # Fade the last 0.3 s to silence so it ends cleanly.
    fade = min(1.0, (LENGTH - t) / 0.3)
    value = max(-1.0, min(1.0, sample * fade))
    frames += struct.pack('<h', int(value * 32767))

with wave.open(sys.argv[1], 'wb') as out:
    out.setnchannels(1)
    out.setsampwidth(2)
    out.setframerate(RATE)
    out.writeframes(bytes(frames))
print("wrote", sys.argv[1], len(frames), "bytes of audio")
