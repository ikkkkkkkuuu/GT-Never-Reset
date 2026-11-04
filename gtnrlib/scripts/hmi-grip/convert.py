"""Emit Punchy tuning for the example's neutral primary grips using an original HMI 5.1.1 jar."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile
import zipfile

import numpy as np


RELEASE = "cb8c8301fcd51ed255a3d77bfcb85a0426d1a222"
ROOT = "resourcepacks/pack_test/assets/minecraft/holdmyitems/"
EXAMPLE = Path(__file__).resolve().parents[2] / "examples/first-person/assets/minecraft/punchy"
# Longitudinal grip point on the extruded 1.7 sprite, expressed in renderItemIn2D coordinates.
GRIPS = {
    "pickaxe": ("global", "gtnrlib_grip", (.78, .22)),
    "hoe": ("HOE", "gtnrlib_grip", (.78, .22)),
    "axe": ("AXE", "gtnrlib_carry_low", (.785, .215)),
    "shovel": ("SHOVEL", "gtnrlib_grip", (.78, .22)),
    "sword": ("SWORD", "gtnrlib_grip", (.875, .125)),
    "ingot": ("GTNRLIB_FLAT_ITEM", "gtnrlib_hold", (.66, .28)),
}


def translation(x, y, z):
    matrix = np.eye(4)
    matrix[:3, 3] = (x, y, z)
    return matrix


def rotation(axis, degrees):
    matrix = np.eye(4)
    i, j = (axis + 1) % 3, (axis + 2) % 3
    sine, cosine = np.sin(np.radians(degrees)), np.cos(np.radians(degrees))
    matrix[i, i] = matrix[j, j] = cosine
    matrix[j, i], matrix[i, j] = sine, -sine
    return matrix


def scale(factor):
    return np.diag([factor, factor, factor, 1.])


def unit(vector):
    return vector / np.linalg.norm(vector)


def axes(matrix):
    return matrix[:3, :3] / np.linalg.norm(matrix[:3, 0])


def euler(matrix):
    # GL postmultiplies Z, Y, X. None of the supported grips is at the Y singularity.
    return np.degrees([np.arctan2(matrix[2, 1], matrix[2, 2]),
                       np.arcsin(np.clip(-matrix[2, 0], -1, 1)),
                       np.arctan2(matrix[1, 0], matrix[0, 0])])


def transform(operation, values):
    if operation == "translate":
        return translation(*values)
    if operation == "scale":
        return np.diag([*values, 1])
    if operation == "shear" and not any(values):
        return np.eye(4)
    if operation.startswith("move"):
        return translation(*(np.eye(3)["XYZ".index(operation[-1])] * values[0]))
    if operation.startswith("rotate"):
        pivot = np.array(values[1:] or [0, 0, 0])
        return translation(*pivot) @ rotation("XYZ".index(operation[-1]), values[0]) @ translation(*-pivot)
    raise ValueError(f"unsupported neutral transform {operation} {values}")


def run_scripts(kind, archive, directory, java):
    command = [java, "-cp", os.pathsep.join([str(directory / "luaj.jar"), str(archive)]),
               "lua", str(Path(__file__).resolve().with_name("sample.lua")), kind]
    run = subprocess.run(command, cwd=directory, capture_output=True, text=True, check=True)
    if not run.stdout.rstrip().endswith("DONE") or "LuaError" in run.stderr:
        raise RuntimeError(run.stderr)
    layers = {}
    for line in run.stdout.splitlines()[:-1]:
        name, *arguments = line.split()
        if name == "BRANCH":
            current = arguments[0]
            layers[current] = np.eye(4)
        else:
            layers[current] = layers[current] @ transform(name, list(map(float, arguments)))
    return layers


def modern_rig(layers, kind):
    # Nonempty right-hand branches of 5.1.1 HeldItemRendererMixin. Scene transforms precede the two draws.
    pivot, undo = translation(.5, .5, .5), translation(-.5, -.5, -.5)
    arm_mount = translation(1.5, -.3, -.6) @ pivot @ rotation(0, 15) @ rotation(1, 35) @ rotation(2, -65) @ undo
    vanilla_arm = (translation(.64, -.6, -.72) @ rotation(1, 45) @ translation(-1, 3.6, 3.5)
                   @ rotation(2, 120) @ rotation(0, 200) @ rotation(1, -135) @ translation(5.6, 0, 0))
    arm = (layers["shared"] @ layers["arm"] @ arm_mount @ scale(.9) @ vanilla_arm
           @ translation(-5 / 16, 2 / 16, 0) @ rotation(2, np.degrees(.1)))
    # HMI submits the first-person hand's stack using THIRD_PERSON_RIGHT_HAND (intermediary field_4320).
    display = (translation(0, 3 / 16, 1 / 16) @ scale(.55) if kind == "ingot" else
               translation(0, 4 / 16, .5 / 16) @ rotation(1, -90) @ rotation(2, 55) @ scale(.85))
    item_mount = translation(.5, -.15, -.85) @ pivot @ rotation(0, 15) @ undo
    item = layers["shared"] @ item_mount @ scale(.9) @ layers["item"] @ display
    return arm, item


def basis(direction, normal):
    return np.column_stack((direction, np.cross(normal, direction), normal))


def tune(kind, animations, arm, item):
    group, clip, uv = GRIPS[kind]
    rx, ry, rz = animations[clip]["bones"]["right_arm"]["rotation"]
    # Work in the common wrist frame, so idle height and translations of the action do not shift the grip.
    bone = rotation(1, 45) @ rotation(2, rz) @ rotation(1, -ry) @ rotation(0, -rx)
    mount = rotation(1, -45) @ rotation(2, 43) @ rotation(0, -32)
    arm_rotation = axes(bone).T @ axes(arm) @ axes(mount).T
    arm_pose = np.eye(4)
    arm_pose[:3, :3] = arm_rotation
    arm_scale = .97
    fist = arm_pose @ scale(arm_scale) @ translation(.04, 0, .04) @ mount @ scale(1.95)
    grip = fist @ [.005, -.065, 0, 1]

    # Constrain the handle to enter the fist transversely. Keep the upstream face twist about that shaft.
    forearm = unit(arm[:3, 1])
    shaft = unit(item[:3, :3] @ [1, 1, 0])
    shaft = unit(shaft - forearm * (shaft @ forearm))
    normal = unit(item[:3, 2] - shaft * (item[:3, 2] @ shaft))
    item_mount = rotation(1, 32)
    sprite = rotation(1, 50) @ rotation(2, -25)
    if normal @ (axes(bone @ item_mount @ sprite)[:, 2]) < 0:
        normal = -normal
    # The old renderer reverses sprite U. Its tool handle runs from bottom-right to top-left in mesh space.
    source = basis(unit(np.array([-1., 1., 0.])), [0, 0, 1])
    item_rotation = axes(bone @ item_mount).T @ basis(shaft, normal) @ source.T @ axes(sprite).T
    item_pose = np.eye(4)
    item_pose[:3, :3] = item_rotation
    item_scale = np.linalg.norm(item[:3, 0]) / (.4 * 1.5)
    card = translation(0, -.3, 0) @ scale(1.5) @ sprite @ translation(-.9375, -.0625, 0)
    anchor = np.array([*uv, -.03, 1])
    offset = (item_mount.T @ grip - item_pose @ scale(item_scale) @ card @ anchor)[:3]

    def pose(orientation, size, shift=None):
        result = dict(zip(("offX", "offY", "offZ"), shift.round(3))) if shift is not None else {}
        result.update(zip(("rotX", "rotY", "rotZ"), euler(orientation).round(2)))
        result["scale"] = round(float(size), 3)
        return result

    return group, pose(arm_rotation, arm_scale), pose(item_rotation, item_scale, offset)


def check_geometry(kind, arm, item):
    def tuning(pose):
        return (translation(*(pose.get("off" + axis, 0) for axis in "XYZ"))
                @ rotation(2, pose.get("rotZ", 0)) @ rotation(1, pose.get("rotY", 0))
                @ rotation(0, pose.get("rotX", 0)) @ scale(pose["scale"]))

    # Rebuild both draw paths from the rounded JSON, cancelling their shared right_arm bone.
    fist = (tuning(arm) @ translation(.04, 0, .04) @ rotation(1, -45)
            @ rotation(2, 43) @ rotation(0, -32) @ scale(1.95))
    card = (rotation(1, 32) @ tuning(item) @ translation(0, -.3, 0) @ scale(1.5)
            @ rotation(1, 50) @ rotation(2, -25) @ translation(-.9375, -.0625, 0))
    relative = np.linalg.inv(fist) @ card
    center = relative @ [*GRIPS[kind][2], -.03, 1]
    shaft = unit(relative[:3, :3] @ [-1, 1, 0])
    if abs(shaft[1]) > np.sin(np.radians(.1)) or np.linalg.norm(center[:3] - [.005, -.065, 0]) > .002:
        raise SystemExit(f"{kind} does not pass transversely through the fist")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jar", type=Path, help="HMI 5.1.1 for Minecraft 1.21.11")
    parser.add_argument("--java", default="java")
    parser.add_argument("--check", action="store_true", help="compare against the active example tuning")
    args = parser.parse_args()
    archive = args.jar.resolve()
    if hashlib.sha1(archive.read_bytes()).hexdigest() != RELEASE:
        parser.error("unsupported release: expected Modrinth KZm1qTcU")
    animations = json.loads((EXAMPLE / "animations/gtnrlib.json").read_text(encoding="utf-8"))["animations"]
    rules = json.loads((EXAMPLE / "compat/gtnrlib.json").read_text(encoding="utf-8"))
    result = {}
    with tempfile.TemporaryDirectory() as work:
        directory = Path(work)
        with zipfile.ZipFile(archive) as jar:
            for script in ("hand_pose", "hand_relative_pose", "item_pose"):
                (directory / (script + ".lua")).write_bytes(jar.read(ROOT + script + ".lua"))
            (directory / "luaj.jar").write_bytes(jar.read("META-INF/jars/luaj-jse-3.0.1.jar"))
        for kind in GRIPS:
            layers = run_scripts(kind, archive, directory, args.java)
            group, arm, item = tune(kind, animations, *modern_rig(layers, kind))
            result[group] = {"arm": arm, "item": item}
            if args.check:
                entry = rules["item"].get(group, {})
                check_geometry(kind, entry.get("armTransforms", rules["globalArm"])["right"],
                               entry.get("transforms", rules["globalItem"])["right"])
                for part, generated, fallback in (("armTransforms", arm, "globalArm"), ("transforms", item, "globalItem")):
                    actual = entry.get(part, rules[fallback])["right"]
                    if any(abs(actual.get(key, 0) - value) > (.015 if key.startswith("rot") else .0006)
                           for key, value in generated.items()):
                        raise SystemExit(f"{kind} {part} differs from converted pose")
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
