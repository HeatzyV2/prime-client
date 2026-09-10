#!/usr/bin/env python3
"""Scaffold Prime Client version layers from existing templates."""
from __future__ import annotations

import shutil
from pathlib import Path

ROOT = Path("/workspace")

# module, package, mc, fabric_api, template, prop_prefix
LAYERS = [
    ("mc-1.21.4", "v1_21_4", "1.21.4", "0.119.4+1.21.4", "mc-1.21.11", "mc1214"),
    ("mc-1.21.5", "v1_21_5", "1.21.5", "0.128.2+1.21.5", "mc-1.21.11", "mc1215"),
    ("mc-1.21.6", "v1_21_6", "1.21.6", "0.128.2+1.21.6", "mc-1.21.11", "mc1216"),
    ("mc-1.21.7", "v1_21_7", "1.21.7", "0.129.0+1.21.7", "mc-1.21.11", "mc1217"),
    ("mc-1.21.8", "v1_21_8", "1.21.8", "0.136.1+1.21.8", "mc-1.21.11", "mc1218"),
    ("mc-1.21.9", "v1_21_9", "1.21.9", "0.134.1+1.21.9", "mc-1.21.11", "mc1219"),
    ("mc-1.21.10", "v1_21_10", "1.21.10", "0.138.4+1.21.10", "mc-1.21.11", "mc12110"),
    ("mc-26.1", "v26_1", "26.1", "0.145.1+26.1", "mc-26.2", "mc261"),
]


def scaffold(module: str, pkg: str, mc: str, fabric_api: str, template: str, prop: str) -> None:
    src_root = ROOT / template
    dst_root = ROOT / module
    if dst_root.exists():
        print(f"skip existing {module}")
        return

    if template == "mc-1.21.11":
        src_pkg, src_mc, src_archives, src_prop = (
            "dev.primeclient.v1_21_11",
            "1.21.11",
            "prime-client-1.21.11",
            "mc12111",
        )
    else:
        src_pkg, src_mc, src_archives, src_prop = (
            "dev.primeclient.v26_2",
            "26.2",
            "prime-client-26.2",
            "mc262",
        )

    dst_pkg = f"dev.primeclient.{pkg}"
    dst_archives = f"prime-client-{mc}"
    print(f"scaffold {module} from {template} → {dst_pkg}")

    shutil.copytree(
        src_root,
        dst_root,
        ignore=shutil.ignore_patterns("build", ".gradle", "bin", "out", "run", "*.class"),
    )

    src_java = dst_root / "src/main/java" / Path(*src_pkg.split("."))
    dst_java = dst_root / "src/main/java" / Path(*dst_pkg.split("."))
    if src_java.exists() and src_java != dst_java:
        dst_java.parent.mkdir(parents=True, exist_ok=True)
        shutil.move(str(src_java), str(dst_java))
        parent = src_java.parent
        while parent != dst_root / "src/main/java" and parent.exists() and not any(parent.iterdir()):
            parent.rmdir()
            parent = parent.parent

    replacements = [
        (src_pkg, dst_pkg),
        (src_pkg.replace(".", "/"), dst_pkg.replace(".", "/")),
        (src_archives, dst_archives),
        (f"{src_prop}_minecraft_version", f"{prop}_minecraft_version"),
        (f"{src_prop}_fabric_api_version", f"{prop}_fabric_api_version"),
        (f"~{src_mc}", f"~{mc}"),
        (f"Minecraft {src_mc}", f"Minecraft {mc}"),
        (f"minecraft {src_mc}", f"minecraft {mc}"),
        (src_mc, mc),  # last, broad
    ]

    for path in dst_root.rglob("*"):
        if not path.is_file():
            continue
        if path.suffix not in {".java", ".gradle", ".json", ".md", ".txt", ".accesswidener"}:
            continue
        text = path.read_text(encoding="utf-8")
        new = text
        for old, rep in replacements:
            new = new.replace(old, rep)
        if new != text:
            path.write_text(new, encoding="utf-8")


def main() -> None:
    for row in LAYERS:
        scaffold(*row)
    print("done")


if __name__ == "__main__":
    main()
