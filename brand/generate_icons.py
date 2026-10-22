#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# João Pedro G M Silva - PUC Goiás ADS - 20251012000740
"""Gera os recursos de ícone do BrainOut a partir de `brand/brainout-icon.svg`.

A marca vive em um único SVG (`brand/brainout-icon.svg`). Este script é o
único lugar onde a geometria é convertida para os formatos que o Android
consome, para que ícone do launcher, arte dentro do app e PNGs legados
nunca saiam de sincronia.

Saídas:

| Arquivo                                              | Grade | Papel |
|------------------------------------------------------|------:|-------|
| `core/ui/.../drawable/neo_art_app_logo.xml`          |   120 | arte dentro do app (contrato NB-05) |
| `app/.../drawable/ic_launcher_background.xml`        |   108 | campo do adaptive icon |
| `app/.../drawable/ic_launcher_foreground.xml`        |   108 | marca do adaptive icon |
| `app/.../drawable/ic_launcher_monochrome.xml`        |   108 | ícone temático (Android 13+) |
| `app/.../mipmap-*/ic_launcher{,_round}.png`          | 48–192 | API 24/25 |
| `brand/brainout-icon-preview.png`                    |   512 | pré-visualização do tile |

Uso:

```bash
python brand/generate_icons.py
```

Dependências: `cairosvg` (renderização SVG -> PNG) e `Pillow` (máscaras
de canto e círculo dos PNGs legados).
"""

from __future__ import annotations

import io
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from pathlib import Path

try:
    import cairosvg
except ImportError:  # pragma: no cover - depende do ambiente
    sys.exit("cairosvg ausente: `python -m pip install cairosvg`")

try:
    from PIL import Image, ImageDraw
except ImportError:  # pragma: no cover - depende do ambiente
    sys.exit("Pillow ausente: `python -m pip install Pillow`")

REPO_ROOT = Path(__file__).resolve().parents[1]
SOURCE_SVG = REPO_ROOT / "brand" / "brainout-icon.svg"

# Grades. A arte do `:core:ui` usa 120 unidades (1 unidade = 1 dp); o
# adaptive icon usa a grade oficial de 108 dp do Android.
ART_VIEWPORT = 120
LAUNCHER_VIEWPORT = 108
LAUNCHER_SCALE = LAUNCHER_VIEWPORT / ART_VIEWPORT

# Adaptive icon: o sistema recorta a camada em 72 dp de 108 dp. A marca
# (ficha com contorno) tem 46 dp de largura visível — 64% da zona visível.
ADAPTIVE_VISIBLE = 72
MARK_VISIBLE_ART = 50

# Densidades dos PNGs legados (API 24/25, antes do adaptive icon).
LEGACY_DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

AUTHOR_HEADER = "<!-- João Pedro G M Silva - PUC Goiás ADS - 20251012000740 -->"
XML_DECLARATION = '<?xml version="1.0" encoding="utf-8"?>'
SVG_NS = "{http://www.w3.org/2000/svg}"

# Cores da paleta A (DESIGN.md §3), em notação Android.
INK = "#FF181818"
YELLOW = "#FFF9D94A"
VIOLET = "#FFB5A1F5"
TRANSPARENT = "#00000000"

SVG_TO_ANDROID = {
    "#181818": INK,
    "#F9D94A": YELLOW,
    "#B5A1F5": VIOLET,
    "#FFFFFF": "#FFFFFFFF",
    "none": TRANSPARENT,
}


@dataclass
class Shape:
    """Uma forma do SVG, já normalizada para a grade de destino."""

    kind: str
    points: list[tuple[float, float]]
    fill: str
    stroke: str | None
    stroke_width: float | None
    role: str = ""

    def scaled(self, factor: float) -> "Shape":
        """Replica a forma com todas as coordenadas multiplicadas por [factor]."""
        return Shape(
            kind=self.kind,
            points=[(x * factor, y * factor) for x, y in self.points],
            fill=self.fill,
            stroke=self.stroke,
            stroke_width=None if self.stroke_width is None else self.stroke_width * factor,
            role=self.role,
        )

    def moved(self, dx: float, dy: float) -> "Shape":
        """Replica a forma transladada por ([dx], [dy])."""
        return Shape(
            kind=self.kind,
            points=[(x + dx, y + dy) for x, y in self.points],
            fill=self.fill,
            stroke=self.stroke,
            stroke_width=self.stroke_width,
            role=self.role,
        )


@dataclass
class Brand:
    """A marca lida do SVG: campo do tile e as formas que compõem o glifo."""

    field: Shape
    mark: list[Shape] = field(default_factory=list)


def parse_color(value: str | None) -> str | None:
    """Normaliza uma cor do SVG para a notação Android (`#AARRGGBB`)."""
    if value is None:
        return None
    value = value.strip()
    if value == "none":
        return None
    if value not in SVG_TO_ANDROID:
        raise SystemExit(f"cor fora da paleta A: {value}")
    return SVG_TO_ANDROID[value]


def parse_points(value: str) -> list[tuple[float, float]]:
    """Lê a lista de pontos de uma `polyline`/`polygon`."""
    numbers = [float(chunk) for chunk in value.replace(",", " ").split()]
    if len(numbers) % 2 != 0:
        raise SystemExit(f"lista de pontos ímpar: {value}")
    return list(zip(numbers[::2], numbers[1::2]))


def read_brand(svg_path: Path) -> Brand:
    """Lê o SVG de origem e devolve o campo e as formas da marca."""
    root = ET.parse(svg_path).getroot()
    groups = {group.get("id"): group for group in root.findall(f"{SVG_NS}g")}

    def shapes_of(group_id: str) -> list[Shape]:
        shapes: list[Shape] = []
        for element in groups[group_id]:
            tag = element.tag.replace(SVG_NS, "")
            fill = parse_color(element.get("fill"))
            stroke = parse_color(element.get("stroke"))
            width = element.get("stroke-width")
            stroke_width = float(width) if width else None
            if tag == "rect":
                x = float(element.get("x", "0"))
                y = float(element.get("y", "0"))
                w = float(element.get("width"))
                h = float(element.get("height"))
                points = [(x, y), (x + w, y), (x + w, y + h), (x, y + h)]
            elif tag in ("polyline", "polygon"):
                points = parse_points(element.get("points", ""))
            else:
                raise SystemExit(f"forma não suportada no SVG da marca: {tag}")
            shapes.append(
                Shape(
                    kind=tag,
                    points=points,
                    fill=fill or TRANSPARENT,
                    stroke=stroke,
                    stroke_width=stroke_width,
                    role=element.get("id", ""),
                )
            )
        return shapes

    return Brand(field=shapes_of("tile")[0], mark=shapes_of("marca"))


def fmt(value: float) -> str:
    """Formata um número para o `pathData` sem ruído de ponto flutuante."""
    text = f"{value:.2f}".rstrip("0").rstrip(".")
    return text if text not in ("", "-0") else "0"


def rect_path(shape: Shape) -> str:
    """`pathData` de um retângulo (canto reto, como o resto da arte Neo)."""
    (x0, y0), (x1, _), (x2, y2), _ = shape.points
    return f"M{fmt(x0)},{fmt(y0)} H{fmt(x1)} V{fmt(y2)} H{fmt(x0)} Z"


def polyline_path(shape: Shape) -> str:
    """`pathData` de uma polilinha aberta."""
    head = f"M{fmt(shape.points[0][0])},{fmt(shape.points[0][1])}"
    tail = " ".join(f"L{fmt(x)},{fmt(y)}" for x, y in shape.points[1:])
    return f"{head} {tail}"


def shape_path(shape: Shape) -> str:
    """`pathData` da forma, conforme o tipo."""
    if shape.kind == "rect":
        return rect_path(shape)
    return polyline_path(shape)


def vector_xml(
    shapes: list[Shape],
    viewport: int,
    size: str,
    comment: str,
) -> str:
    """Monta um `VectorDrawable` com as formas dadas, na ordem em que aparecem."""
    lines = [
        XML_DECLARATION,
        AUTHOR_HEADER,
        f"<!-- {comment} -->",
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        f'    android:width="{size}"',
        f'    android:height="{size}"',
        f'    android:viewportWidth="{viewport}"',
        f'    android:viewportHeight="{viewport}">',
    ]
    for shape in shapes:
        attributes = [f'android:fillColor="{shape.fill}"']
        attributes.append(f'android:pathData="{shape_path(shape)}"')
        if shape.stroke is not None and shape.stroke_width is not None:
            attributes.append(f'android:strokeColor="{shape.stroke}"')
            attributes.append(f'android:strokeWidth="{fmt(shape.stroke_width)}"')
        lines.append("    <path")
        lines.extend(f"        {attribute}" for attribute in attributes[:-1])
        lines.append(f"        {attributes[-1]} />")
    lines.append("</vector>")
    return "\n".join(lines) + "\n"


def write(path: Path, content: str) -> None:
    """Grava o arquivo criando o diretório quando preciso."""
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8", newline="\n")
    print(f"  {path.relative_to(REPO_ROOT)}")


def write_png(path: Path, image: Image.Image) -> None:
    """Grava um PNG e anuncia o arquivo."""
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, format="PNG", optimize=True)
    print(f"  {path.relative_to(REPO_ROOT)}")


def compose_svg(brand: Brand, mark_scale: float, canvas: int = ART_VIEWPORT) -> str:
    """Serializa o tile: campo chapado + marca escalada sobre o centro.

    [mark_scale] é a escala da marca (na grade de 120) para que ela ocupe a
    mesma fração do canvas que ocupa no adaptive icon.
    """
    center = canvas / 2
    parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{fmt(canvas)}" '
        f'height="{fmt(canvas)}" viewBox="0 0 {fmt(canvas)} {fmt(canvas)}">',
        f'    <rect x="0" y="0" width="{fmt(canvas)}" height="{fmt(canvas)}" '
        f'fill="#{brand.field.fill[3:]}"/>',
    ]
    for shape in brand.mark:
        scaled = shape.scaled(mark_scale)
        shifted = scaled.moved(center - ART_VIEWPORT / 2 * mark_scale, center - ART_VIEWPORT / 2 * mark_scale)
        if shape.kind == "rect":
            (x0, y0), (x1, _), (x2, y2), _ = shifted.points
            parts.append(
                f'    <rect x="{fmt(x0)}" y="{fmt(y0)}" '
                f'width="{fmt(x2 - x0)}" height="{fmt(y2 - y0)}" '
                f'fill="#{shifted.fill[3:]}"'
                + (
                    f' stroke="#{shifted.stroke[3:]}" stroke-width="{fmt(shifted.stroke_width)}"'
                    if shifted.stroke
                    else ""
                )
                + "/>"
            )
        else:
            points = " ".join(f"{fmt(x)},{fmt(y)}" for x, y in shifted.points)
            parts.append(
                f'    <polyline points="{points}" fill="none" '
                f'stroke="#{shifted.stroke[3:]}" stroke-width="{fmt(shifted.stroke_width)}"/>'
            )
    parts.append("</svg>")
    return "\n".join(parts)


def render_png(svg: str, size: int) -> Image.Image:
    """Renderiza o SVG em PNG RGBA do tamanho pedido."""
    png = cairosvg.svg2png(
        bytestring=svg.encode("utf-8"),
        output_width=size,
        output_height=size,
    )
    return Image.open(io.BytesIO(png)).convert("RGBA")


def mask_image(image: Image.Image, shape: str) -> Image.Image:
    """Aplica a máscara do tile: quadrado arredondado ou círculo."""
    size = image.size[0]
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    if shape == "circle":
        draw.ellipse((0, 0, size - 1, size - 1), fill=255)
    else:
        radius = round(size * 0.18)
        draw.rounded_rectangle((0, 0, size - 1, size - 1), radius=radius, fill=255)
    output = image.copy()
    output.putalpha(mask)
    return output


def mark_scale_for(canvas_units: int) -> float:
    """Escala da marca (grade de 120) para um canvas de [canvas_units].

    A marca tem 50 unidades visíveis na grade de 120 e 46 na grade de 108 —
    a mesma proporção que tem dentro da zona visível de 72 dp do adaptive
    icon. Com [canvas_units] = 108 reproduzimos o adaptive icon inteiro; com
    72 reproduzimos o tile já recortado, que é o que o PNG legado mostra.
    """
    return (46 / MARK_VISIBLE_ART) * (ART_VIEWPORT / canvas_units)


def main() -> None:
    brand = read_brand(SOURCE_SVG)

    # 1) Arte dentro do app (`:core:ui`): a marca na grade de 120, sem o
    #    campo — ela senta sobre o fundo do tema.
    write(
        REPO_ROOT / "core/ui/src/main/res/drawable/neo_art_app_logo.xml",
        vector_xml(
            brand.mark,
            ART_VIEWPORT,
            "120dp",
            "Ícone do BrainOut (arte dentro do app) — marca neobrutalista: ficha "
            "yellow com contorno ink de 6 unidades, marca de conclusão e barra "
            "violeta sobre sombra rígida +8,+8. Gerado por brand/generate_icons.py "
            "a partir de brand/brainout-icon.svg.",
        ),
    )

    # 2) Adaptive icon: campo violeta + marca, na grade de 108 dp.
    scaled = [shape.scaled(LAUNCHER_SCALE) for shape in brand.mark]
    scaled_field = brand.field.scaled(LAUNCHER_SCALE)

    write(
        REPO_ROOT / "app/src/main/res/drawable/ic_launcher_background.xml",
        vector_xml(
            [scaled_field],
            LAUNCHER_VIEWPORT,
            "108dp",
            "Campo do ícone do BrainOut: violeta da paleta A, chapado — a "
            "identidade é a marca, não o fundo.",
        ),
    )

    write(
        REPO_ROOT / "app/src/main/res/drawable/ic_launcher_foreground.xml",
        vector_xml(
            scaled,
            LAUNCHER_VIEWPORT,
            "108dp",
            "Marca do ícone do BrainOut na zona segura de 108 dp do adaptive "
            "icon. Gerado por brand/generate_icons.py a partir de "
            "brand/brainout-icon.svg.",
        ),
    )

    # 3) Camada monocromática (ícone temático do Android 13+): só o traço,
    #    sem preenchimento — o sistema aplica a cor do tema.
    outline = [
        Shape(
            kind=shape.kind,
            points=shape.points,
            fill=TRANSPARENT,
            stroke=shape.stroke,
            stroke_width=shape.stroke_width,
            role=shape.role,
        )
        for shape in scaled
        if shape.stroke is not None
    ]
    write(
        REPO_ROOT / "app/src/main/res/drawable/ic_launcher_monochrome.xml",
        vector_xml(
            outline,
            LAUNCHER_VIEWPORT,
            "108dp",
            "Camada monocromática do ícone do BrainOut (ícone temático do "
            "Android 13+): só o contorno da ficha e a marca de conclusão, sem "
            "preenchimento — o sistema pinta com a cor do tema.",
        ),
    )

    # 4) Pré-visualização: o adaptive icon inteiro (campo + marca), que é o
    #    que o lançador mostra depois do recorte.
    preview = render_png(compose_svg(brand, mark_scale_for(LAUNCHER_VIEWPORT)), 512)
    write_png(REPO_ROOT / "brand/brainout-icon-preview.png", preview)

    # 5) PNGs legados (API 24/25): o tile já recortado — o campo ocupa a
    #    imagem inteira e a marca mantém os 64% da zona visível.
    legacy_svg = compose_svg(brand, mark_scale_for(ADAPTIVE_VISIBLE))
    for density, size in LEGACY_DENSITIES.items():
        for variant, shape in (("ic_launcher", "rounded"), ("ic_launcher_round", "circle")):
            image = mask_image(render_png(legacy_svg, size), shape)
            write_png(
                REPO_ROOT / f"app/src/main/res/mipmap-{density}/{variant}.png",
                image,
            )


if __name__ == "__main__":
    main()
