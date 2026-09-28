# BrainOut — Protótipo web (wireframes)

Protótipo estático (HTML5 + CSS puro, sem JavaScript) das **oito telas** do
aplicativo, em alta fidelidade com a identidade Material 3 atual do projeto
(paleta e tipografia de `core/ui/.../theme/`, sem dynamic color). Serve como
referência visual para os marcos **E1.3** (navegação entre telas) e **E1.6**
(login/cadastro) definidos em [`../ROADMAP.md`](../ROADMAP.md).

> **Prévia visual apenas.** O site não autentica, não persiste dados e não
> faz requisições de rede. Os conteúdos (projetos, tarefas, percentuais)
> são de demonstração.

## Telas incluídas

| Arquivo               | Tela                | Marco relacionado |
|-----------------------|---------------------|-------------------|
| `splash.html`         | Splash / Boas-vindas | E1.3             |
| `login.html`          | Login                | E1.6             |
| `register.html`       | Cadastro             | E1.6             |
| `home.html`           | Projetos (Home)      | E1.3 / E2.6      |
| `project-detail.html` | Detalhe do projeto   | E1.3 / E2.4      |
| `tasks.html`          | Tarefas              | E2.6             |
| `dashboard.html`      | Painel               | E2.7             |
| `settings.html`       | Configurações        | E1.3             |

Há também:

- `index.html` — hub com as oito telas e o link para a galeria;
- `lo-fi.html` — **«Visão geral do protótipo»**: galeria com iframes das oito
  telas, fichas dos estados *Home — Member* e *Offline*, e a legenda do fluxo.
  O nome do arquivo foi mantido para não quebrar links existentes;
- `styles.css` — sistema visual compartilhado (paleta clara/escura, moldura
  responsiva, componentes);
- `icons.svg` — sprite local dos quatro ícones da barra inferior (mais o de
  busca), referenciados via `<use href="icons.svg#…">`.

## Como abrir

Os arquivos são 100% estáticos. Qualquer uma das opções abaixo funciona:

```bash
# 1) Abrir o índice no navegador padrão (Linux)
xdg-open index.html

# 2) Servir via HTTP local (útil se o navegador bloquear file://)
python3 -m http.server 8000
# Em seguida, acesse http://localhost:8000/
```

> Não há dependências de build, CDN, framework ou JavaScript.
> O CSS está centralizado em `styles.css`.

## Convenções aplicadas

- **HTML5 semântico** (`<main>`, `<header>`, `<section>`, `<nav>`, `<ul>`,
  `<fieldset>`, `<legend>`).
- **CSS compartilhado** em um único arquivo (`styles.css`); os únicos estilos
  inline são dados de demonstração (cor de tag, altura das barras do gráfico).
- **Viewport:** de 320 px a desktop. Abaixo de 480 px a moldura vira
  *full-bleed* (`100dvh`, sem *notch*); acima disso fica centralizada no
  palco, com largura `min(100%, 400px)`.
- **Tema claro/escuro** via `prefers-color-scheme`, espelhando `Color.kt`.
- **Acessibilidade:** área mínima de 44×44 px nos controles, foco visível
  (`:focus-visible`), `aria-current="page"` na aba ativa, elementos visuais
  não textuais com `aria-hidden`, e chips informativos como texto (não
  botões falsos).
- **Navegação entre páginas** via `<a href>` (sem `href="#"`):
  `splash → login → cadastro → login → projetos → detalhe → tarefas →
  painel → configurações → login`.
- **Sem dependências externas:** fonte do sistema, SVG local, zero CDN.

## Limitações

- Autenticação, sincronização offline e notificações **não** rodam no site —
  o protótipo apenas navega entre páginas.
- Os estados *Member* e *Offline* são fichas ilustrativas na galeria, não
  telas navegáveis.
