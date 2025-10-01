#!/usr/bin/env python3
import csv
import json
import argparse
from pathlib import Path

SRC = Path("glyph_eyes_animation_corrected.csv")
DEFAULT_DST = Path("glyph_eyes_animation_slow.json")

MATRIX_W = 25
MATRIX_H = 25
DEFAULT_DURATION_MS = 400  # デフォルト（1フレーム400ms）


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
    parser = argparse.ArgumentParser(description="Convert 25x25 CSV frames to timeline JSON for GlyphEyes")
    parser.add_argument("--duration", type=int, default=DEFAULT_DURATION_MS, help="Duration per frame in ms (default: 400)")
    parser.add_argument("--dst", type=str, default=str(DEFAULT_DST), help="Output JSON filepath")
    parser.add_argument("--easing", type=str, default="easeInOutSine", help="Easing name for frames")
    parser.add_argument("--loop", action="store_true", help="Enable loop flag in JSON (default: False unless specified)")
    parser.add_argument("--repeat", type=int, default=1, help="Repeat each input frame N times to slow down even if duration is ignored (default: 1)")
    args = parser.parse_args()

    frames = read_frames_from_csv(SRC)
    timeline = []
    t = 0
    seq_index = 0
    for _, pixels in enumerate(frames):
        for _r in range(max(1, args.repeat)):
            timeline.append({
                "index": seq_index,
                "startMs": t,
                "durationMs": args.duration,
                "easing": args.easing,
                "pixels": pixels,
            })
            t += args.duration
            seq_index += 1

    out = {
        "format": "GlyphEyesFramesV1",
        "matrix": {"width": MATRIX_W, "height": MATRIX_H},
        "loop": bool(args.loop),
        "totalDurationMs": t,
        "timeline": timeline,
        "legend": {"off": 0, "on": 2040}
    }

    dst_path = Path(args.dst)
    dst_path.write_text(json.dumps(out, ensure_ascii=False, indent=2))
    print(f"Wrote {dst_path} with {len(frames)} frames, {args.duration}ms per frame (total {t}ms), easing={args.easing}, loop={bool(args.loop)}")


if __name__ == "__main__":
    main()
