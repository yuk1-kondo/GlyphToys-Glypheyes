#!/usr/bin/env python3
import argparse
from pathlib import Path

def main():
    parser = argparse.ArgumentParser(description="Repeat each frame row in a 25x25 CSV to slow down animation")
    parser.add_argument("--src", type=str, default="glyph_eyes_animation_corrected.csv", help="Input CSV path")
    parser.add_argument("--dst", type=str, required=True, help="Output CSV path")
    parser.add_argument("--repeat", type=int, default=2, help="Repeat count per frame (default: 2)")
    args = parser.parse_args()

    src = Path(args.src)
    dst = Path(args.dst)
    rep = max(1, int(args.repeat))

    lines = src.read_text().strip().splitlines()
    out_lines = []
    for line in lines:
        for _ in range(rep):
            out_lines.append(line)

    dst.write_text("\n".join(out_lines) + "\n")
    print(f"Wrote {dst} with {len(lines)} source frames repeated x{rep} => {len(out_lines)} total frames")

if __name__ == "__main__":
    main()
