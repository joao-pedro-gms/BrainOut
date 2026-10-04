# Licenças dos assets do `:core:ui`

Índice dos textos de licença que acompanham os binários empacotados em
`core/ui/src/main/res/`. Proveniência detalhada (origem fixada por commit,
ferramenta, versão, sha256 e como reproduzir) está em
[`docs/design/ASSETS.md`](../../docs/design/ASSETS.md).

| Arquivo | Assets cobertos | Licença | Origem do texto |
|---|---|---|---|
| `OFL-1.1-Archivo.txt` | `res/font/archivo_semi_bold.ttf`, `archivo_bold.ttf`, `archivo_extra_bold.ttf` | SIL Open Font License 1.1 | `google/fonts` → `ofl/archivo/OFL.txt` |
| `OFL-1.1-PublicSans.txt` | `res/font/public_sans_regular.ttf`, `public_sans_semi_bold.ttf` | SIL Open Font License 1.1 | `google/fonts` → `ofl/publicsans/OFL.txt` |
| `Apache-2.0-Material-Symbols.txt` | `res/drawable/neo_ic_*.xml` (16 ícones) | Apache License 2.0 | `google/material-design-icons` @ `737e3324305806514d7909874fa1818ae1808232` → `LICENSE` |

Os textos são cópias literais dos arquivos de origem — nada foi reescrito.
A presença e o conteúdo são asserts de `NeoFontAssetsTest`; apagar ou
alterar um destes arquivos quebra a suíte de `:core:ui`.

A arte original (`res/drawable/neo_art_*.xml`) é obra do projeto e não
carrega licença de terceiro. Nenhum asset de `:core:ui` é baixado em
runtime: tudo é empacotado no APK.
