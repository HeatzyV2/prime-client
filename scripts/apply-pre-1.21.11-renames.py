#!/usr/bin/env python3
"""Apply Minecraft ≤1.21.10 Mojmap renames to scaffolded version layers.

1.21.11 renamed several symbols (Identifier, GraphicsPreset, PlayerModel package,
RenderTypes package). Layers copied from 1.21.11 need the inverse for 1.21.4–1.21.10.
"""

from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VERSIONS = [
    "mc-1.21.4",
    "mc-1.21.5",
    "mc-1.21.6",
    "mc-1.21.7",
    "mc-1.21.8",
    "mc-1.21.9",
    "mc-1.21.10",
]


def transform(text: str) -> str:
    # Identifier → ResourceLocation (class + import)
    text = text.replace(
        "import net.minecraft.resources.Identifier;",
        "import net.minecraft.resources.ResourceLocation;",
    )
    # Avoid replacing inside comments mentioning Identifier as a word carefully:
    # Replace type usages of Identifier with ResourceLocation, but not in strings
    # that are unrelated. Simple whole-word replace is fine for our codebase.
    text = re.sub(r"\bIdentifier\b", "ResourceLocation", text)

    # GraphicsPreset → GraphicsStatus + method rename
    text = text.replace(
        "import net.minecraft.client.GraphicsPreset;",
        "import net.minecraft.client.GraphicsStatus;",
    )
    text = re.sub(r"\bGraphicsPreset\b", "GraphicsStatus", text)
    text = text.replace("graphicsPreset()", "graphicsMode()")

    # PlayerModel package moved in 1.21.11
    text = text.replace(
        "import net.minecraft.client.model.player.PlayerModel;",
        "import net.minecraft.client.model.PlayerModel;",
    )

    # RenderTypes package → RenderType (1.21.11 nested package)
    text = text.replace(
        "import net.minecraft.client.renderer.rendertype.RenderTypes;",
        "import net.minecraft.client.renderer.RenderType;",
    )
    text = re.sub(r"\bRenderTypes\b", "RenderType", text)

    # jspecify is not on older MC classpaths — strip annotation usage
    text = text.replace("import org.jspecify.annotations.Nullable;\n", "")
    text = re.sub(r"@Nullable\s+", "", text)

    return text


def main() -> None:
    changed = 0
    for version in VERSIONS:
        java_root = ROOT / version / "src" / "main" / "java"
        if not java_root.is_dir():
            print(f"skip missing {version}")
            continue
        for path in java_root.rglob("*.java"):
            original = path.read_text(encoding="utf-8")
            updated = transform(original)
            if updated != original:
                path.write_text(updated, encoding="utf-8")
                changed += 1
                print(f"updated {path.relative_to(ROOT)}")
    print(f"done, {changed} files changed")


if __name__ == "__main__":
    main()
