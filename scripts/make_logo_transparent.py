#!/usr/bin/env python3
"""
Make white/near-white background of Tamixa logo PNG transparent.
Usage: python3 make_logo_transparent.py <input.png> <output.png> [fuzz_percent]
"""
import sys
from pathlib import Path

from PIL import Image

def main():
    if len(sys.argv) < 3:
        print("Usage: make_logo_transparent.py <input.png> <output.png> [fuzz_percent]", file=sys.stderr)
        sys.exit(1)
    input_path = Path(sys.argv[1])
    output_path = Path(sys.argv[2])
    fuzz = int(sys.argv[3]) if len(sys.argv) > 3 else 18  # percent tolerance for "white"

    img = Image.open(input_path).convert("RGBA")
    data = img.getdata()
    new_data = []
    threshold = 255 * (100 - fuzz) / 100  # e.g. 18% fuzz -> pixels with R,G,B > ~209 become transparent

    for item in data:
        r, g, b, a = item
        if r >= threshold and g >= threshold and b >= threshold:
            new_data.append((255, 255, 255, 0))
        else:
            new_data.append(item)

    img.putdata(new_data)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(output_path, "PNG")
    print(f"Saved: {output_path}")

if __name__ == "__main__":
    main()
