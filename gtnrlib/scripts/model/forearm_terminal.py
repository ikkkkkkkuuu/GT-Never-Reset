"""Author the GTNR wrist terminal in sixteenth-block units and export the editable OBJ."""

import argparse
import math
from collections import defaultdict
from pathlib import Path

# Only opaque regions of the two existing atlas files are sampled.
UV = {
    "graphite": (19, 0, 25, 4),
    "olive": (12, 0, 18, 4),
    "steel": (0, 0, 6, 1),
    "brass": (26, 11, 32, 15),
    "latch": (11, 16, 17, 19),
    "rib": (22, 5, 28, 10),
    "signal": (15, 15, 16, 16),
    "cyan": (3, 15, 5, 17),
    "lens": (0, 15, 2, 17),
    "amber": (24, 15, 25, 16),
}


def add(a, b):
    return tuple(x + y for x, y in zip(a, b))


def scale(v, f):
    return tuple(x * f for x in v)


class Mesh:
    def __init__(self):
        self.vertices = {}
        self.uvs = {}
        self.groups = defaultdict(list)

    def face(self, group, material, points, reverse=False):
        u0, v0, u1, v1 = UV[material]
        u0, u1 = (u0 + 0.14) / 32, (u1 - 0.14) / 32
        v0, v1 = 1 - (v0 + 0.14) / 32, 1 - (v1 - 0.14) / 32
        uv = ((u0, v1), (u1, v1), (u1, v0), (u0, v0))
        if len(points) == 3:
            uv = ((u0, v1), (u1, v1), ((u0 + u1) / 2, v0))
        vertex_ids = [
            self.vertices.setdefault(tuple(round(v / 16, 6) for v in p), len(self.vertices) + 1)
            for p in points
        ]
        uv_ids = [self.uvs.setdefault(pair, len(self.uvs) + 1) for pair in uv]
        for a, b, c in [(0, 1, 2)] + ([(0, 2, 3)] if len(points) == 4 else []):
            indices = (a, c, b) if reverse else (a, b, c)
            self.groups[group].append("f " + " ".join(f"{vertex_ids[i]}/{uv_ids[i]}" for i in indices))

    def loft(self, group, rings, materials, reverse=False):
        for before, after, material in zip(rings, rings[1:], materials):
            for i in range(len(before)):
                j = (i + 1) % len(before)
                self.face(group, material, (before[i], before[j], after[j], after[i]), reverse)

    def cap(self, group, ring, material, reverse=False):
        center = tuple(sum(p[axis] for p in ring) / len(ring) for axis in range(3))
        for i in range(len(ring)):
            self.face(group, material, (ring[i], ring[(i + 1) % len(ring)], center), reverse)

    def write(self, path):
        with path.open("w", encoding="utf-8", newline="\n") as out:
            out.write("# GTNR forearm terminal - original shaped cuff and separately machined controls.\n")
            out.writelines("v %.6f %.6f %.6f\n" % p for p in self.vertices)
            out.write("\n")
            out.writelines("vt %.6f %.6f\n" % p for p in self.uvs)
            for group, faces in self.groups.items():
                out.write("\ng " + group + "\n")
                out.write("\n".join(faces) + "\n")


def cuff_section(y, rx, rz, exponent):
    result = []
    for i in range(32):
        theta = i * math.tau / 32
        x, z = math.cos(theta), math.sin(theta)
        result.append((
            math.copysign(rx * abs(x) ** (2 / exponent), x),
            y, math.copysign(rz * abs(z) ** (2 / exponent), z),
        ))
    return result


def cuff(mesh, name, profile, materials):
    # A closed wall cross-section. The final interior surface returns to the first lip.
    rings = [cuff_section(*station) for station in profile]
    mesh.loft(name, rings + [rings[0]], materials, reverse=True)


def rounded_rectangle(cx, cy, rx, ry, corner, depth):
    steps = 2 if corner < .15 else 4
    return [
        (cx + sx * (rx - corner) + corner * math.cos(angle),
         cy + sy * (ry - corner) + corner * math.sin(angle), depth)
        for quadrant, (sx, sy) in enumerate(((1, 1), (-1, 1), (-1, -1), (1, -1)))
        for angle in (quadrant * math.pi / 2 + step * (math.pi / 2) / (steps - 1) for step in range(steps))
    ]


def plate(mesh, group, cx, cy, stations, materials, front, back="graphite"):
    rings = [rounded_rectangle(cx, cy, rx, ry, corner, z) for z, rx, ry, corner in stations]
    mesh.loft(group, rings, materials)
    mesh.cap(group, rings[0], front, reverse=True)
    mesh.cap(group, rings[-1], back)


def panel_ring(mesh, group, cx, cy, profile, materials):
    rings = [rounded_rectangle(cx, cy, rx, ry, corner, z) for z, rx, ry, corner in profile]
    mesh.loft(group, rings + [rings[0]], materials)


def wheel(mesh, group, center, yaw, profile, side_materials, cap_material, teeth=0, segments=32):
    u = (math.cos(yaw), 0, math.sin(yaw))
    v = (0, 1, 0)
    axis = (math.sin(yaw), 0, -math.cos(yaw))

    def ring(depth, radius, toothed):
        points = []
        for i in range(segments):
            theta = i * math.tau / segments
            length = radius - (teeth if toothed and i % 2 == 0 else 0)
            point = add(scale(u, math.cos(theta) * length), scale(v, math.sin(theta) * length))
            points.append(add(add(center, scale(axis, depth)), point))
        return points

    # Rings progress towards the viewer along the axle; reverse orientation for the outward face.
    rings = [ring(*station) for station in profile]
    mesh.loft(group, rings, side_materials, True)
    mesh.cap(group, rings[-1], cap_material, True)
    mesh.cap(group, rings[0], "graphite")


def build(mesh):
    cuff(mesh, "liner", [
        (-4.34, 2.62, 2.60, 6), (-4.30, 2.93, 2.96, 4),
        (-3.89, 3.02, 3.02, 4), (2.60, 2.94, 2.97, 4), (2.89, 2.76, 2.79, 4),
        (2.93, 2.55, 2.55, 6), (2.63, 2.51, 2.51, 6), (-4.02, 2.59, 2.57, 6),
    ], ["graphite", "graphite", "graphite", "steel", "graphite", "graphite", "graphite", "graphite"])

    cuff(mesh, "shell", [
        (-4.12, 2.94, 3.01, 4), (-3.97, 3.32, 3.36, 3.4), (-3.60, 3.62, 3.76, 3.2),
        (-2.45, 3.78, 3.86, 3.35), (1.62, 3.60, 3.64, 3.4), (2.56, 3.18, 3.30, 3.8),
        (2.71, 2.94, 2.99, 4), (2.59, 2.88, 2.94, 4), (-3.98, 2.95, 3.00, 4),
    ], ["steel", "olive", "steel", "olive", "olive", "steel", "graphite", "graphite", "graphite"])

    for front, rear, outer in [(-3.80, -3.36, 3.79), (1.66, 2.14, 3.69)]:
        cuff(mesh, "end_ribs", [
            (front, outer - 0.13, outer - 0.05, 3.4),
            (front + 0.07, outer, outer + 0.08, 3.4),
            (rear - 0.07, outer, outer + 0.08, 3.4),
            (rear, outer - 0.16, outer - 0.09, 3.4),
        ], ["graphite", "rib", "steel", "graphite"])

    plate(mesh, "console", -0.64, -0.73, [
        (-5.13, 2.51, 2.52, .61), (-4.80, 2.79, 2.72, .71), (-3.73, 2.77, 2.81, .77),
        (-2.97, 2.40, 2.53, .72),
    ], ["steel", "olive", "steel"], "graphite")
    # Open bevel around the inset screen, with a recessed backplate.
    panel_ring(mesh, "bezel", -0.64, -0.82, [
        (-5.08, 2.39, 2.13, .55), (-5.38, 2.22, 2.04, .49),
        (-5.38, 1.85, 1.67, .35), (-5.14, 1.69, 1.54, .28),
        (-5.08, 1.69, 1.54, .28),
    ], ["steel", "olive", "graphite", "graphite", "steel"])
    plate(mesh, "display", -0.64, -0.82, [
        (-5.155, 1.68, 1.53, .26), (-5.09, 1.69, 1.54, .26),
    ], ["signal"], "signal")

    plate(mesh, "controls", 2.27, -.41, [
        (-4.29, .91, 2.86, .61), (-4.05, 1.11, 2.94, .65), (-2.80, 1.03, 2.68, .56),
    ], ["steel", "olive"], "graphite")
    wheel(mesh, "dial", (2.25, 1.25, -4.25), math.radians(13), [
        (0, .86, False), (.19, .93, False), (.42, .88, True), (.74, .88, True),
        (.85, .75, False),
    ], ["graphite", "brass", "brass", "steel"], "graphite", .075)
    plate(mesh, "dial", 2.20, .97, [(-5.11, .08, .17, .06), (-5.075, .10, .18, .06)],
          ["brass"], "latch")
    for y in (-2.48, -1.62, -.76):
        panel_ring(mesh, "controls", 2.28, y, [
            (-4.64, .58, .29, .12), (-4.51, .66, .33, .12),
            (-4.23, .56, .32, .11), (-4.22, .47, .21, .09),
        ], ["steel", "graphite", "graphite", "graphite"])
        plate(mesh, "buttons", 2.28, y, [
            (-4.79, .43, .19, .085), (-4.66, .51, .24, .095), (-4.32, .49, .22, .095),
        ], ["brass", "graphite"], "brass")

    # Raised optical turret sits between the screen and the proximal collar.
    plate(mesh, "emitter", 0.28, -3.55, [
        (-5.34, 1.16, .62, .31), (-5.04, 1.26, .75, .36), (-3.77, 1.19, .88, .40),
    ], ["steel", "olive"], "graphite")
    wheel(mesh, "emitter", (0.28, -3.55, -5.26), 0, [
        (0, .53, False), (.20, .57, False), (.375, .47, False),
    ], ["brass", "steel"], "graphite")
    wheel(mesh, "lens", (0.28, -3.55, -5.26), 0, [
        (.376, .34, False), (.450, .31, False),
    ], ["cyan"], "lens")

    # The vent slats sit inside a recessed channel on the far side of the arm.
    plate(mesh, "service_plate", -.55, -.82, [
        (3.78, 2.07, 2.46, .34), (4.01, 1.85, 2.24, .28),
    ], ["steel"], "olive", "olive")
    plate(mesh, "service_plate", -.55, -.63, [
        (4.02, 1.58, 1.16, .24), (4.13, 1.44, .99, .22),
    ], ["steel"], "graphite")
    for x in (-1.64, -1.20, -.76, -.32, .12, .56):
        plate(mesh, "cooling_fins", x, -.63, [
            (4.14, .118, .80, .075), (4.29, .116, .73, .075),
        ], ["steel"], "rib", "rib")

    for y in (-3.48, 1.96):
        plate(mesh, "catch_seats", 2.42, y, [
            (2.15, .99, .53, .20), (2.81, .92, .53, .19), (3.04, .74, .44, .17),
        ], ["olive", "brass"], "steel")
        for x in (1.9, 2.9):
            plate(mesh, "latches", x, y, [
                (2.88, .17, .44, .10), (3.24, .15, .38, .08),
            ], ["steel"], "brass")
        plate(mesh, "latches", 2.42, y, [
            (3.03, .53, .16, .12), (3.30, .47, .13, .09),
        ], ["brass"], "latch")

    for x, y in [(-2.38,-2.25),(-2.42,.65),(1.15,.78),(-.63,1.59)]:
        wheel(mesh, "fasteners", (x, y, -5.10), 0, [
            (0, .13, False), (.11, .12, False),
        ], ["steel"], "brass", segments=10)

    for y in (-1.52, -.92, -.32):
        plate(mesh, "status_lamps", -2.91, y, [
            (-5.147, .105, .13, .07), (-5.08, .125, .155, .07),
        ], ["amber"], "amber")
    # A simple static reticle on the small local monitor; the actual UI is projected separately.
    for x, y, rx, ry in [(-.94, -1.66, .76, .045), (-1.17, -1.37, .53, .035),
                         (-1.25, -1.11, .45, .035), (-.9, .05, .75, .025),
                         (.64, -.65, .046, .56), (.42, -.08, .16, .045)]:
        plate(mesh, "display_marks", x, y, [
            (-5.162, rx, ry, min(rx, ry) * .32), (-5.155, rx, ry, min(rx, ry) * .32),
        ], ["cyan"], "cyan")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path)
    options = parser.parse_args()
    mesh = Mesh()
    build(mesh)
    mesh.write(options.output)
