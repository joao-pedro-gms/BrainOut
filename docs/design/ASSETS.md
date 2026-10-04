# Ativos visuais do redesign — fontes, ícones e arte (NB-05)

> Documento de proveniência. Toda fonte, ícone e arte que o `:core:ui`
> empacota tem aqui: origem fixada, licença, ferramenta, versão e como
> reproduzir. Se um asset não estiver documentado, ele não deveria estar no
> APK.

Plano de referência: [redesign neobrutalista](../plans/2026-10-01-redesign-neobrutalista.md)
— tarefa NB-05, seção 4 do `DESIGN.md`. Requisito: **nenhuma fonte remota,
licenças rastreáveis, primeira abertura 100% offline.**

Inventário entregue em `core/ui/src/main/res/`:

| Conjunto | Arquivos | Bytes | Licença |
|---|---:|---:|---|
| Fontes estáticas (`res/font/`) | 5 `.ttf` | 276 184 | SIL OFL 1.1 |
| Ícones (`res/drawable/neo_ic_*.xml`) | 16 | 18 092 | Apache-2.0 |
| Arte original (`res/drawable/neo_art_*.xml`) | 4 | 5 183 | obra do projeto |
| **Total (teto Q28 = 1 MB)** | **25** | **299 459** | — |

O teto é verificado por `NeoVectorAssetsTest` (`a soma de fontes e
drawables cabe no teto de 1 MB`), que falha se o total passar de
1 048 576 bytes.

---

## 1. Fontes — instâncias estáticas derivadas do `google/fonts`

### 1.1 Por que derivar

`google/fonts` publica **só** variantes variáveis em `ofl/archivo` e
`ofl/publicsans` (`Archivo[wdth,wght].ttf`, `PublicSans[wght].ttf`) — não
existe arquivo estático por peso para baixar. O `minSdk 24` inclui API
24/25, onde suporte a fonte variável não existe; embutir só a variável
faria o app cair no fallback nessas versões. A decisão do `DESIGN.md` §4 é
instanciar localmente, registrar origem/ferramenta/versão e validar.

### 1.2 Origem (fixada por commit + sha256)

| Fonte | URL fixada | sha256 do `.ttf` variável |
|---|---|---|
| Archivo | `raw.githubusercontent.com/google/fonts/6c70c829f09ea345d3590406693220ea35c6553f/ofl/archivo/Archivo%5Bwdth%2Cwght%5D.ttf` | `0e094a7d3c7c4c25cf1310c4b30014f1dae9332220b1c2c88f4fa996f0b05053` |
| Public Sans | `raw.githubusercontent.com/google/fonts/37caf657942042c2de8f5f11d9d6866a1b689b84/ofl/publicsans/PublicSans%5Bwght%5D.ttf` | `d75a7dc1a27eb9e336d5b33f55489d2ecb5621bf694d5c43b2415bce2ca830a8` |

Os commits são o último commit do `google/fonts` que tocou cada arquivo;
o sha256 confirma que o binário baixado é exatamente este.

### 1.3 Arquivos empacotados

| Recurso | Família / estilo (`name`) | `OS/2.usWeightClass` | Bytes | sha256 |
|---|---|---:|---:|---|
| `archivo_semi_bold` | Archivo SemiBold | 600 | 65 740 | `f9e74c7315a97a5010b23f99aeb3b5245af0e7554298ecd9d64f4c102b401ffd` |
| `archivo_bold` | Archivo Bold | 700 | 65 648 | `20da9c395ec197de9dc2b7d80b5b654abe5271af2d3aa156d48f6157f1c764b3` |
| `archivo_extra_bold` | Archivo ExtraBold | 800 | 65 696 | `b99d026574d239a862f7e852b827fd68e326883f65cd44b58bd2d3877a0746fe` |
| `public_sans_regular` | Public Sans Regular | 400 | 39 500 | `a67f2dfd7ffa6178cfdcdee5fe2c1fcfecc3e0da3c5288c8869f659d7d7acd6a` |
| `public_sans_semi_bold` | Public Sans SemiBold | 600 | 39 600 | `deab244ebbb26f0ee3a5ac95faebca703892e0fab1bc9bc2736c699b1c22ede7` |

Nenhum arquivo tem a tabela `fvar` — são instâncias estáticas de verdade,
não um rótulo por cima da variável (`NeoFontAssetsTest` falha se `fvar`
aparecer).

### 1.4 Como foram derivadas

Ferramenta: **fontTools 4.66.1** (Python 3.14.7), `varLib.instancer` +
`subset`. Procedimento, na ordem:

```bash
python3 -m pip install fonttools==4.66.1

# 1) fixa os eixos e atualiza a tabela name (família/estilo canônicos)
python3 -m fontTools.varLib.instancer Archivo\[wdth,wght\].ttf \
    wght=600 wdth=100 --update-name-table --no-recalc-timestamp \
    -o archivo_semi_bold.ttf
python3 -m fontTools.varLib.instancer Archivo\[wdth,wght\].ttf \
    wght=700 wdth=100 --update-name-table --no-recalc-timestamp -o archivo_bold.ttf
python3 -m fontTools.varLib.instancer Archivo\[wdth,wght\].ttf \
    wght=800 wdth=100 --update-name-table --no-recalc-timestamp -o archivo_extra_bold.ttf
python3 -m fontTools.varLib.instancer PublicSans\[wght\].ttf \
    wght=400 --update-name-table --no-recalc-timestamp -o public_sans_regular.ttf
python3 -m fontTools.varLib.instancer PublicSans\[wght\].ttf \
    wght=600 --update-name-table --no-recalc-timestamp -o public_sans_semi_bold.ttf

# 2) recorte para o que a UI usa, mantendo TODOS os recursos de layout
UNICODES='U+0020-007E,U+00A0-00FF,U+0100-017F,U+2000-206F,U+20A0-20BF,U+2122,U+2212'
python3 -m fontTools.subset archivo_semi_bold.ttf --unicodes="$UNICODES" \
    --layout-features='*' --name-IDs='*' --name-legacy --name-languages='*' \
    --notdef-outline --output-file=archivo_semi_bold.ttf
# ... repita para os outros quatro
```

Escopo do recorte: ASCII, Latin-1 (acentos de pt-BR), Latin Extended-A,
pontuação tipográfica (travessão, aspas, reticências, bullet), moeda (€),
`™` e `−`. **Fora de propósito:** vietnamita/latino adicional e símbolos
matemáticos (`≥`, `≤`) — nenhum deles aparece em string do app.
`--layout-features='*'` mantém `tnum`, `lnum`, `onum`, `liga` etc.

Dois caracteres que aparecem em `strings.xml` ficam **fora** do recorte,
decisão registrada aqui: `→` (U+2192, em `home_sort_name_asc/desc`) e
`⚠` (U+26A0, em `project_detail_new_task_holiday_hint`). Archivo tem `→`
upstream e Public Sans não; `⚠` não existe em nenhuma das duas — deixar
os dois de fora dá o **mesmo** fallback do sistema nas duas famílias, em
vez de `→` sair em Archivo num trecho e não sair no outro. As telas neo
devem preferir um ícone local (`neo_ic_*`) a `⚠` no texto. Todo o resto
dos não-ASCII usados pelo app é asserção de teste — `todos os nao-ascii das strings do app existem nas fontes salvo as excecoes`.

Reexecutar o pipeline produz arquivos **semanticamente idênticos**
(mesma tabela `name`, mesmo `usWeightClass`, mesmo `cmap`, mesmos glifos,
mesmo `GSUB`) com bytes eventualmente distintos em `head`/`hhea`/`maxp`,
onde mora o carimbo de tempo. A verificação autoritativa não é o sha256:
é a suíte indicada na seção 5.

### 1.5 Recursos tipográficos confirmados

| Recurso | Archivo | Public Sans |
|---|---|---|
| Dígitos tabulares (`tnum`) | sim | sim |
| Versalete proporcional/ordinal (`pnum`, `onum`, `ordn`) | sim | sim |
| Ligaduras (`liga`) | sim | sim |

O Painel pode usar `fontFeatureSettings = "tnum"` nas duas famílias —
`DESIGN.md` §4 exige dígitos tabulares «se o arquivo escolhido suportar»,
e ele suporta. Cobertura verificada em runtime pelo teste
`as fontes cobrem pt-BR e EN sem fallback`, que falha se algum glifo
esperado faltar (foi assim que a ausência de `≥/≤` foi detectada e o
escopo do recorte foi documentado).

---

## 2. Ícones — Material Symbols Outlined locais

### 2.1 Origem

- Repositório: `google/material-design-icons` @
  `737e3324305806514d7909874fa1818ae1808232` (02/10/2026) — commit fixado,
  não `master`.
- Licença: Apache-2.0, texto em
  `core/ui/licenses/Apache-2.0-Material-Symbols.txt`.
- Variante: **`<ícone>_wght500_24px.svg`** — peso visual 500 e opção 24,
  como manda `DESIGN.md` §5. Sem o token `fill1` e sem `grad*` no nome do
  arquivo: um só peso, um só estilo.
- Padrão de URL:
  `raw.githubusercontent.com/google/material-design-icons/<commit>/symbols/web/<ícone>/materialsymbolsoutlined/<ícone>_wght500_24px.svg`

Só o que o app usa está exportado — não há fonte de símbolos completa nem
catálogo inteiro no APK.

### 2.2 Manifesto (manifesto fechado: mudar exige atualizar o teste)

| Recurso Android | Símbolo | Uso atual no código | `autoMirrored` |
|---|---|---|---|
| `neo_ic_add` | `add` | `Icons.Filled.Add` (FAB, novo projeto/tarefa) | não |
| `neo_ic_arrow_back` | `arrow_back` | `Icons.AutoMirrored.Outlined.ArrowBack` | **sim** |
| `neo_ic_assignment` | `assignment` | `Icons.AutoMirrored.Outlined.Assignment` (aba Tarefas) | **sim** |
| `neo_ic_bar_chart` | `bar_chart` | `Icons.Outlined.BarChart` (aba Painel) | não |
| `neo_ic_chevron_right` | `chevron_right` | `Icons.Outlined.ChevronRight` | não |
| `neo_ic_close` | `close` | `Icons.Filled.Clear` (limpar busca) | não |
| `neo_ic_cloud_off` | `cloud_off` | `Icons.Outlined.CloudOff` | não |
| `neo_ic_cloud_sync` | `cloud_sync` | `Icons.Outlined.CloudSync` | não |
| `neo_ic_delete` | `delete` | `Icons.Outlined.Delete` | não |
| `neo_ic_folder` | `folder` | `Icons.Outlined.Folder` (aba Projetos) | não |
| `neo_ic_logout` | `logout` | `Icons.AutoMirrored.Outlined.Logout` | **sim** |
| `neo_ic_more_vert` | `more_vert` | `Icons.Outlined.MoreVert` | não |
| `neo_ic_settings` | `settings` | `Icons.Outlined.Settings` (aba Ajustes) | não |
| `neo_ic_sort` | `sort` | `Icons.Outlined.Sort` / `Icons.AutoMirrored.Outlined.Sort` | **sim** |
| `neo_ic_visibility` | `visibility` | `Icons.Filled.Visibility` (senha) | não |
| `neo_ic_visibility_off` | `visibility_off` | `Icons.Filled.VisibilityOff` | não |

Duas notas de nomenclatura:

- `Icons.Filled.Clear` **não** existe como `symbols/web/clear`: o símbolo
  equivalente é `close`, mesmo glifo X. Por isso o recurso chama-se
  `neo_ic_close`.
- Espelhar só o que o código atual marca como `AutoMirrored` — símbolo de
  estado (`folder`, `settings`, `bar_chart`…) não é espelhado
  indiscriminadamente (`DESIGN.md` §5).

### 2.3 Regra de conversão SVG → VectorDrawable

Os SVGs oficiais usam `viewBox="0 -960 960 960"`; `VectorDrawable` não
tem origem deslocável, então o deslocamento é feito por grupo:

```xml
<vector android:width="24dp" android:height="24dp"
    android:viewportWidth="960" android:viewportHeight="960"
    android:autoMirrored="true">        <!-- só nos quatro da tabela -->
    <group android:translateY="960">    <!-- -(-960) -->
        <path android:pathData="…" android:fillColor="#FF000000" />
    </group>
</vector>
```

- `pathData` é o atributo `d` do SVG **copiado sem redesenho**;
- `fillColor` é literal preto: quem aplica cor é o tint do Compose;
- todos os 16 SVGs têm um único `<path>` e nenhum declara `fill-rule`, então
  não há `fillType` a preservar;
- comandos usados: `M m L l H h V v Q q T t Z` — todos suportados pelo
  parser de `VectorDrawable`.

Verificação de proveniência de um ícone (substitua `<ícone>`):

```bash
curl -s "https://raw.githubusercontent.com/google/material-design-icons/737e3324305806514d7909874fa1818ae1808232/symbols/web/<ícone>/materialsymbolsoutlined/<ícone>_wght500_24px.svg" \
  | grep -o 'd="[^"]*"' | sed 's/^d="//; s/"$//' > /tmp/svg-d.txt
grep -o 'android:pathData="[^"]*"' core/ui/src/main/res/drawable/neo_ic_<ícone>.xml \
  | sed 's/.*pathData="//; s/"$//' > /tmp/xml-d.txt
diff /tmp/svg-d.txt /tmp/xml-d.txt && echo "pathData idêntico"
```

---

## 3. Arte geométrica original (poster estático)

Quatro artes autorais, nenhuma de terceiro (sem asset de logo, mascote ou
ilustração de biblioteca — `DESIGN.md` §5 proíbe reutilizar material de
terceiros):

| Recurso | Uso previsto | Bytes |
|---|---|---:|
| `neo_art_splash_poster` | apresentação/splash — «ficha no carimbo» | 1 175 |
| `neo_art_empty_projects` | estado vazio de Projetos (pasta) | 1 128 |
| `neo_art_empty_tasks` | estado vazio de Tarefas (ficha) | 1 516 |
| `neo_art_empty_dashboard` | estado vazio do Painel (barras) | 1 364 |

Regras aplicadas (todas verificadas por `NeoVectorAssetsTest`):

- viewport `120×120` com `width/height = 120dp` — **1 unidade = 1dp**, então
  a ilustração cabe no limite de `≤120dp` do `BrainOutStatePanel`;
- no máximo **três cores** por arte, todas da paleta A (`ink`, `yellow`,
  `violet`, `blue`, `white`);
- traço uniforme de **6 unidades** em todo arquivo;
- **um único offset** de sombra (`+8,+8`), sem blur — é a mesma assinatura
  de sombra rígida do NB-04;
- sem texto rasterizado, sem imagem, sem rede.

São o **poster estático** de Q28: não existe animação associada a nenhum
deles, então a política de motion zero (NB-07) já é satisfeita por
construção. Se em NB-25 entrar uma ilustração animada, o asset animado é
adicional e este poster continua sendo o fallback.

Cores literais nos XMLs porque `VectorDrawable` não lê tokens Kotlin; o
mapeamento para os papéis semânticos está em `DESIGN.md` §3.

---

## 4. Licenças (`core/ui/licenses/`)

| Arquivo | Cobre | Texto de origem |
|---|---|---|
| `OFL-1.1-Archivo.txt` | `res/font/archivo_*.ttf` | `google/fonts/ofl/archivo/OFL.txt` |
| `OFL-1.1-PublicSans.txt` | `res/font/public_sans_*.ttf` | `google/fonts/ofl/publicsans/OFL.txt` |
| `Apache-2.0-Material-Symbols.txt` | `res/drawable/neo_ic_*.xml` | `google/material-design-icons/LICENSE` @ `737e332…` |

Nenhuma das duas famílias declara *Reserved Font Name* (a tabela `name`
só traz copyright e marca de Omnibus-Type), então derivar instâncias
mantendo o nome da família é compatível com a OFL 1.1 §3. Os textos são
cópias literais dos arquivos de origem, verificados pelos testes
(`as licencas OFL estao rastreaveis ao lado das fontes`).

A arte `neo_art_*` é obra do projeto (autor: João Pedro G M Silva), não
carrega licença de terceiro.

---

## 5. Como verificar

```bash
# contratos: fontes, ícones, arte e os 15 estilos tipográficos
./gradlew :core:ui:testDebugUnitTest

# nada de fonte remota: só R.font.* locais
grep -rn "http" core/ui/src/main/res/font/ || echo "sem rede nas fontes"

# orçamento Q28 (bytes de res/font + res/drawable)
du -cb core/ui/src/main/res/font core/ui/src/main/res/drawable | tail -1
```

Casos de teste que sustentam as afirmações deste documento:

| Teste | O que prova |
|---|---|
| `NeoFontAssetsTest` | cinco arquivos certos, instância estática sem `fvar`, peso real, cobertura pt-BR/EN, não-ASCII das `strings.xml` do app salvo as exceções documentadas, licenças OFL presentes |
| `NeoVectorAssetsTest` | manifesto fechado de 16+4, viewport/`autoMirrored`/`pathData` literais, três cores, traço uniforme, teto de 1 MB, cabeçalho de autoria |
| `NeoDrawableInflationTest` | os 20 vetores inflam com o `PathParser` real do Android (Robolectric) e as 5 fontes existem como recurso empacotado |
| `NeoTypographyContractTest` | os 15 estilos batem com a tabela do `DESIGN.md` §4, tudo em `sp`, nenhuma família padrão, papéis de erro/ação/prazo ≥14sp, todo peso existe como arquivo |

Validação de dispositivo (pendência registrada na entrega): primeira
abertura **offline** e escala de fonte 1.0/1.3/2.0 em API 24/25 exigem
emulador/dispositivo — quando disponível, executar
`connectedDevDebugAndroidTest` + inspeção manual.
