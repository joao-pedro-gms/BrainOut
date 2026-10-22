# Marca e ícone do BrainOut

A identidade visual do BrainOut é **neobrutalista**: cor chapada, contorno
grosso de tinta (`ink`), canto reto e sombra impressa — nunca brilho. Este
diretório guarda a **fonte única de verdade** do ícone do aplicativo e o
script que a converte para os formatos que o Android consome.

Nenhum recurso de ícone é desenhado à mão duas vezes: tudo sai de
[`brainout-icon.svg`](brainout-icon.svg).

---

## 1. O glifo

Uma **ficha de tarefa carimbada**: o cartão yellow com contorno ink que
guarda a marca de conclusão, com a barra violeta da tarefa embaixo, tudo
sobre a sombra rígida deslocada. É a mesma linguagem do poster de abertura
(`neo_art_splash_poster`), então ícone do launcher, tela de abertura e
Configurações mostram a mesma figura.

Por que esta forma:

- **Reconhecível a 48 px.** Só há três formas (ficha, marca, barra) e
  nenhum detalhe fino — a barra é o único elemento fino e existe
  justamente porque quebra a silhueta do bloco.
- **Diz o que o app é.** Task manager: a marca de conclusão é o verbão, a
  barra é a lista.
- **Cabe em qualquer máscara.** O glifo inteiro fica dentro do círculo
  seguro de 66 dp do adaptive icon, então máscaras circulares, squircle e
  quadrada mostram a mesma figura.

### Geometria (grade de 120 unidades — 1 unidade = 1 dp na arte)

| Elemento | Forma | Cor |
|---|---|---|
| Sombra | retângulo `46,46 → 90,90` (a ficha deslocada `+8,+8`) | ink `#181818` |
| Ficha | retângulo `38,38 → 82,82`, contorno de 6 unidades | yellow `#F9D94A` + ink |
| Marca de conclusão | polilinha `50,56 → 58,64 → 70,48`, traço de 6 | ink `#181818` |
| Barra da tarefa | retângulo `44,70 → 67,75` | violet `#B5A1F5` |

Regras que a figura cumpre (e que os testes verificam):

- traço uniforme de **6 unidades** em toda a arte;
- **um único** deslocamento de sombra (`+8,+8`), sem blur;
- no máximo **três cores**, todas da paleta A do `DESIGN.md` §3;
- canto reto e `pathData` só com comandos poligonais (`M`, `L`, `H`, `V`, `Z`).

### Paleta

| Papel | Hex | Origem |
|---|---|---|
| ink | `#181818` | `NeoPrimitive.ink` |
| yellow | `#F9D94A` | `NeoPrimitive.yellow` (ação) |
| violet | `#B5A1F5` | `NeoPrimitive.violet` (marca) |

O campo do tile do launcher é o **violeta** chapado: a identidade é a
figura, não o fundo. Dentro do app a arte é **transparente** e senta sobre
o fundo do tema (paper no claro, night no escuro).

---

## 2. Arquivos gerados

| Arquivo | Grade | Papel |
|---|---:|---|
| `core/ui/src/main/res/drawable/neo_art_app_logo.xml` | 120 | arte dentro do app (`:core:ui`, contrato NB-05) |
| `app/src/main/res/drawable/ic_launcher_background.xml` | 108 | campo do adaptive icon |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | 108 | figura do adaptive icon |
| `app/src/main/res/drawable/ic_launcher_monochrome.xml` | 108 | ícone temático (Android 13+) |
| `app/src/main/res/mipmap-*/ic_launcher.png` | 48–192 | API 24/25, quadrado arredondado |
| `app/src/main/res/mipmap-*/ic_launcher_round.png` | 48–192 | API 24/25, circular |
| `brand/brainout-icon-preview.png` | 512 | pré-visualização do tile |

Os PNGs legados mostram o **tile já recortado** (72 dp visíveis): o campo
ocupa a imagem inteira e a figura mantém os 64% da zona visível, exatamente
como no adaptive icon.

---

## 3. Como regenerar

```bash
python brand/generate_icons.py
```

Dependências: `cairosvg` e `Pillow` (`python -m pip install cairosvg Pillow`).

O script **sobrescreve** os arquivos da tabela acima. Ele não inventa
geometria: lê `brainout-icon.svg`, converte para `VectorDrawable` e renderiza
os PNGs. Para mudar a figura, edite o SVG e rode o script — nunca edite os
recursos derivados à mão, porque o próximo `generate_icons.py` desfaz a
mudança.

### O que o script valida

- toda cor do SVG pertence à paleta A (erro caso contrário);
- as formas do SVG são `rect`, `polyline` ou `polygon`;
- as escalas são derivadas de duas constantes — `46` (largura visível da
  figura na grade de 108) e `72` (zona visível do adaptive icon) — para que
  figura e tile nunca fiquem fora de proporção.

---

## 4. Uso dentro do app

| Tela | Uso |
|---|---|
| Abertura (`:feature:auth`) | a arte no lugar do antigo monograma «B» |
| Configurações (`:feature:settings`) | cabeçalho de identidade (figura + nome do app) |

O ícone do **launcher** é o `android:icon` do manifesto (`:app`), e a
tela de abertura do sistema no Android 12+ mostra a mesma figura
automaticamente, porque ela vem do adaptive icon.

---

## 5. Licença

Obra do projeto (autor: João Pedro G M Silva), como as demais artes `neo_art_*`.
Sem material de terceiro: nenhuma fonte de logo, mascote ou ilustração de
biblioteca foi reutilizada (`DESIGN.md` §5).
