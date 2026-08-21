#!/usr/bin/env python3
"""Train and export the single-class YOLOv8 weapon detector for SecureVision.

Produces the file the app expects at:

    ml/ml-weapon/src/main/assets/weapon_detector.tflite

Dataset: https://www.kaggle.com/datasets/alinoorqureshi/weapon-detection-yolo-optimized
One class, "Weapon". The shipped build reached mAP@50 ~0.80.

The detector is single-class by design. It reports THAT a weapon is present and
cannot name the type, which is why the app renders a generic "WEAPON nn%" label
rather than "pistol" or "knife".

Usage:
    pip install ultralytics
    # download + unzip the dataset, then point --data at its data.yaml
    python scripts/export_weapon_yolo.py --data path/to/data.yaml
    python scripts/export_weapon_yolo.py --data path/to/data.yaml --epochs 100

    # skip training and just export weights you already have
    python scripts/export_weapon_yolo.py --weights runs/detect/train/weights/best.pt
"""

from __future__ import annotations

import argparse
import pathlib
import shutil
import sys

DEFAULT_OUT = pathlib.Path("ml/ml-weapon/src/main/assets/weapon_detector.tflite")
DATASET_SLUG = "alinoorqureshi/weapon-detection-yolo-optimized"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--data", type=pathlib.Path, help="dataset data.yaml (required to train)")
    parser.add_argument("--weights", type=pathlib.Path, help="skip training, export these .pt weights")
    parser.add_argument("--base", default="yolov8n.pt", help="base checkpoint (default: yolov8n.pt)")
    parser.add_argument("--epochs", type=int, default=100)
    parser.add_argument("--imgsz", type=int, default=640, help="must stay 640 for this app")
    parser.add_argument("--out", type=pathlib.Path, default=DEFAULT_OUT)
    args = parser.parse_args()

    if args.imgsz != 640:
        raise SystemExit("SecureVision letterboxes to 640x640; --imgsz must be 640.")
    if not args.data and not args.weights:
        raise SystemExit(
            "Provide --data to train, or --weights to export existing weights.\n"
            f"Dataset: https://www.kaggle.com/datasets/{DATASET_SLUG}"
        )

    from ultralytics import YOLO

    if args.weights:
        print(f"Loading {args.weights}")
        model = YOLO(str(args.weights))
    else:
        print(f"Training {args.base} on {args.data} for {args.epochs} epochs...")
        model = YOLO(args.base)
        model.train(data=str(args.data), epochs=args.epochs, imgsz=args.imgsz, single_cls=True)

    print("Exporting to TFLite...")
    exported = pathlib.Path(model.export(format="tflite", imgsz=args.imgsz))

    args.out.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy(exported, args.out)

    size_mb = args.out.stat().st_size / (1024 * 1024)
    print(f"\nWrote {args.out}  ({size_mb:.1f} MB)")
    print(
        "\nNote: ultralytics may emit NHWC [1,640,640,3] rather than NCHW\n"
        "[1,3,640,640]. Both are fine -- the app inspects the input tensor at\n"
        "load and feeds it in whichever layout the model declares.\n"
        "Verify with:  adb logcat -s WeaponModel:V"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
