# BrainOut — Wireframes HTML

Wireframes estáticos (HTML5 + CSS puro, sem JavaScript) das 6 telas base do
aplicativo. Servem como referência visual para os marcos **E1.3** (navegação
entre telas) e **E1.6** (login/cadastro) definidos em [`../ROADMAP.md`](../ROADMAP.md).

> Estes arquivos são apenas referência de fluxo e hierarquia visual.
> A identidade visual definitiva (cores, tipografia, logotipo) será definida
> em momento posterior do Ciclo 1.

## Telas incluídas

| Arquivo                  | Tela                | Marco relacionado |
|--------------------------|---------------------|-------------------|
| `splash.html`            | Splash / Boas-vindas | E1.3             |
| `login.html`             | Login                | E1.6             |
| `register.html`          | Cadastro             | E1.6             |
| `home.html`              | Home (Projetos)      | E1.3             |
| `project-detail.html`    | Detalhe do projeto   | E1.3             |
| `settings.html`          | Configurações        | E1.3             |

Há também `index.html`, um hub opcional que lista todas as telas para
visualização rápida.

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
- **CSS compartilhado** em um único arquivo (`styles.css`); nenhum estilo
  inline além de ajustes pontuais de layout nos mockups.
- **Viewport alvo:** 360×800 px (mobile), com moldura de celular e *notch*
  apenas para indicar onde o conteúdo começaria em um aparelho real.
- **Acessibilidade mínima:** elementos visuais não textuais (FAB, ícones da
  tab bar, avatares, badges) marcados com `aria-label` ou `aria-hidden`
  conforme apropriado.
- **Navegação entre páginas** via `<a href>` para testar o fluxo:
  `splash → login → home → project-detail → settings`.
- **Sem identidade visual final:** paleta neutra (cinza/branco) e sem fontes
  externas; foco apenas em estrutura e hierarquia.

## Próximos passos

- Substituir estes mockups pelas telas reais em Jetpack Compose (módulos
  `:feature:auth` e `:feature:projects`) conforme o E1.3 e o E1.6 avançarem.
- Atualizar `styles.css` para consumir tokens do Material 3 quando o tema for
  definido no E1.7.
