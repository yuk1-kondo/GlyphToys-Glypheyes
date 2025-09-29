#!/usr/bin/env python3
import csv
import json
from pathlib import Path

SRC = Path("glyph_eyes_animation_corrected.csv")
DST = Path("glyph_eyes_animation_slow.json")

MATRIX_W = 25
MATRIX_H = 25
FRAME_DURATION_MS = 400  # ゆっくり（1フレーム400ms）


def read_frames_from_csv(p: Path):
    frames = []
    with p.open("r", newline="") as f:
        reader = csv.reader(f)
        for row in reader:
            if not row:
                continue
            values = list(map(int, row))
            if len(values) != MATRIX_W * MATRIX_H:
                raise ValueError(f"Expected {MATRIX_W*MATRIX_H} values per frame, got {len(values)}")
            # 2Dに整形
            pixels = [values[i:i+MATRIX_W] for i in range(0, len(values), MATRIX_W)]
            frames.append(pixels)
    return frames


def main():
    frames = read_frames_from_csv(SRC)
    timeline = []
    t = 0
    for idx, pixels in enumerate(frames):
        timeline.append({
            "index": idx,
            "startMs": t,
            "durationMs": FRAME_DURATION_MS,
            "easing": "easeInOutSine",
            "pixels": pixels,
        })
        t += FRAME_DURATION_MS

    out = {
        "format": "GlyphEyesFramesV1",
        "matrix": {"width": MATRIX_W, "height": MATRIX_H},
        "loop": True,
        "totalDurationMs": t,
        "timeline": timeline,
        "legend": {"off": 0, "on": 2040}
    }

    DST.write_text(json.dumps(out, ensure_ascii=False, indent=2))
    print(f"Wrote {DST} with {len(frames)} frames, {FRAME_DURATION_MS}ms per frame (total {t}ms)")


if __name__ == "__main__":
    main()
