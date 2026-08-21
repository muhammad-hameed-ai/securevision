#!/usr/bin/env python3
"""Export FaceNet-512 to TFLite for SecureVision.

Produces the file the app expects at:

    ml/ml-face/src/main/assets/facenet_512.tflite

The app requires input [1, 160, 160, 3] and output [1, 512]. A model with any
other shape is rejected at load with an explanatory log rather than producing
silent garbage, so a wrong export fails loudly instead of quietly degrading
recognition.

Usage:
    pip install deepface tensorflow
    python scripts/export_facenet.py
    python scripts/export_facenet.py --out /tmp/facenet_512.tflite
"""

from __future__ import annotations

import argparse
import pathlib
import sys

EXPECTED_INPUT = (1, 160, 160, 3)
EXPECTED_OUTPUT = (1, 512)
DEFAULT_OUT = pathlib.Path("ml/ml-face/src/main/assets/facenet_512.tflite")


def build_keras_model():
    """Return the Keras FaceNet-512 model.

    DeepFace changed this API: older releases returned the Keras model straight
    from build_model, newer ones return a client object wrapping it as .model.
    Both are handled so the script does not break on a routine pip upgrade.
    """
    from deepface import DeepFace

    built = DeepFace.build_model("Facenet512")
    model = getattr(built, "model", built)

    if not hasattr(model, "input_shape"):
        raise SystemExit(
            f"Unexpected object from DeepFace.build_model: {type(built)!r}. "
            "Your deepface version may have changed its API again."
        )
    return model


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--out",
        type=pathlib.Path,
        default=DEFAULT_OUT,
        help=f"output path (default: {DEFAULT_OUT})",
    )
    args = parser.parse_args()

    import tensorflow as tf

    print("Building Facenet512 via deepface (first run downloads weights)...")
    model = build_keras_model()
    print(f"  input  {model.input_shape}")
    print(f"  output {model.output_shape}")

    # Fail here rather than on the phone. A silently wrong embedding size is the
    # hardest class of bug to notice: everything runs, and every face matches
    # everyone equally badly.
    if tuple(model.output_shape) != EXPECTED_OUTPUT:
        raise SystemExit(
            f"Refusing to export: output {model.output_shape} != {EXPECTED_OUTPUT}. "
            "SecureVision is built around 512-dimensional embeddings."
        )

    print("Converting to TFLite...")
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_bytes = converter.convert()

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_bytes(tflite_bytes)

    size_mb = len(tflite_bytes) / (1024 * 1024)
    print(f"\nWrote {args.out}  ({size_mb:.1f} MB)")
    print("Verify on device with:  adb logcat -s FaceEmbedder:V")
    return 0


if __name__ == "__main__":
    sys.exit(main())
