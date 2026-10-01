"""Verifica a proposta de tokens; não certifica a UI Android renderizada."""

import json
import re
from pathlib import Path


def unique_keys(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"Chave JSON duplicada: {key}")
        result[key] = value
    return result


def luminance(hex_color):
    channels = [int(hex_color[i:i + 2], 16) / 255 for i in (1, 3, 5)]
    linear = [
        c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4
        for c in channels
    ]
    return sum(c * weight for c, weight in zip(linear, (0.2126, 0.7152, 0.0722)))


def contrast(foreground, background):
    low, high = sorted((luminance(foreground), luminance(background)))
    return (high + 0.05) / (low + 0.05)


def require(condition, message):
    if not condition:
        raise ValueError(message)


def verify_documents(tokens, directory):
    root_doc = directory.parents[1] / "DESIGN.md"
    documents = [root_doc, directory / "PESQUISA.md", directory / "ESPECIFICACAO.md"]
    for path in documents + [directory / "tokens.json", Path(__file__)]:
        text = path.read_text(encoding="utf-8")
        require(text.endswith("\n"), f"Sem newline final: {path.name}")
        require(all(line == line.rstrip() for line in text.splitlines()), f"Whitespace: {path.name}")
    for path in documents:
        text = path.read_text(encoding="utf-8")
        for link in re.findall(r"\]\(([^)]+)\)", text):
            if link.startswith(("https://", "http://")):
                continue
            target, _, anchor = link.partition("#")
            destination = path.parent / target if target else path
            require(destination.is_file(), f"Link local quebrado: {path.name} → {link}")
            if anchor:
                headings = re.findall(r"^#+\s+(.+)$", destination.read_text(encoding="utf-8"), re.M)
                slugs = [re.sub(r"[^\w\s-]", "", h.lower()).replace(" ", "-") for h in headings]
                require(anchor in slugs, f"Âncora quebrada: {path.name} → {link}")

    section = root_doc.read_text(encoding="utf-8").split("### Tokens semânticos da paleta A\n", 1)[1]
    section = section.split("### Status, prioridades, prazo e tags", 1)[0]
    for line in section.splitlines():
        if not line.startswith("| `"):
            continue
        columns = line.split("|")
        roles = re.findall(r"`([^`]+)`", columns[1])
        for index, mode in ((2, "light"), (3, "dark")):
            colors = re.findall(r"#[0-9A-F]{6}", columns[index])
            if len(colors) == 1:
                colors *= len(roles)
            require(len(roles) == len(colors), f"Linha semântica inválida: {line}")
            for role, color in zip(roles, colors):
                primitive = tokens["themes"][mode][role]
                require(tokens["primitives"][primitive] == color, f"Doc/JSON divergem: {mode}/{role}")
    print("OK: links locais/âncoras, whitespace e tabela semântica Markdown/JSON.")


def main():
    path = Path(__file__).with_name("tokens.json")
    tokens = json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=unique_keys)
    primitives = tokens["primitives"]
    themes = tokens["themes"]
    require(set(themes) == {"light", "dark"}, "Temas devem ser light/dark")
    require(set(themes["light"]) == set(themes["dark"]), "Paridade semântica divergente")
    for name, value in primitives.items():
        require(re.fullmatch(r"#[0-9A-F]{6}", value), f"Hex inválido: {name}")
    for mode, theme in themes.items():
        for role, primitive in theme.items():
            require(primitive in primitives, f"Referência inválida: {mode}/{role}")
        for role in tokens["materialMapping"].values():
            require(role in theme, f"Material role sem token: {mode}/{role}")

    require(set(tokens["recipes"]["status"]) == {"TODO", "DOING", "DONE"}, "Estados divergentes")
    require(
        set(tokens["recipes"]["priority"]) == {"LOW", "MEDIUM", "HIGH", "URGENT", "CRITICAL"},
        "Prioridades divergentes",
    )
    roles = {
        f"{group}{size}"
        for group in ("display", "headline", "title", "body", "label")
        for size in ("Large", "Medium", "Small")
    }
    require(set(tokens["typography"]) == roles, "Escala tipográfica incompleta")
    for role, style in tokens["typography"].items():
        require(style["line"] >= style["size"] > 0, f"Linha/tamanho inválido: {role}")
        require(style["weight"] in range(100, 901, 100), f"Peso inválido: {role}")
    for name, shadow in tokens["shadows"].items():
        require(shadow["blur"] == shadow["spread"] == 0, f"Sombra não rígida: {name}")
    require(tokens["sizes"]["touchMin"] >= 48, "Alvo inferior a 48dp")
    require(tokens["spacing"] == sorted(set(tokens["spacing"])), "Spacing inválido")
    require(all(value % 4 == 0 for value in tokens["spacing"]), "Spacing fora do ritmo")
    require(0 <= tokens["scrimAlpha"] <= 1, "Scrim inválido")
    require(tokens["motion"]["durations"]["instant"] == 0, "Reduced motion precisa ser instantâneo")
    require(abs(contrast("#000000", "#FFFFFF") - 21) < 1e-9, "Fórmula: preto/branco deve ser 21:1")
    require(contrast("#181818", "#181818") == 1, "Fórmula: cor idêntica deve ser 1:1")
    verify_documents(tokens, path.parent)

    checks = list(tokens["contrastChecks"])
    for group in tokens["recipes"].values():
        checks.extend(
            {"foreground": recipe["text"], "background": recipe["background"], "minimum": 4.5}
            for recipe in group.values()
        )
    for state in ("success", "info", "warning", "error"):
        checks.extend(
            {"foreground": state, "background": background, "minimum": 4.5}
            for background in ("background", "surface", "surface.alt")
        )
    checks.extend(
        {"foreground": "border.default", "background": f"{state}.container", "minimum": 3}
        for state in ("success", "info", "warning", "error", "secondary")
    )

    count = 0
    failures = []
    for mode, theme in themes.items():
        ratios = []
        for check in checks:
            fg, bg = check["foreground"], check["background"]
            ratio = contrast(primitives[theme[fg]], primitives[theme[bg]])
            count += 1
            ratios.append(ratio)
            if ratio < check["minimum"]:
                failures.append(f"{mode}: {fg}/{bg} = {ratio:.6f} < {check['minimum']}")
        print(f"{mode}: {len(checks)} pares; menor razão: {min(ratios):.4f}:1")
    require(not failures, "Contraste insuficiente:\n" + "\n".join(failures))
    print(f"OK: {len(primitives)} primitivos, {len(themes['light'])} papéis/tema, 15 tipos, {count} pares.")
    print("Escopo: sRGB opaco e receitas declaradas; pixels, TalkBack e assets exigem QA próprio.")


if __name__ == "__main__":
    main()
