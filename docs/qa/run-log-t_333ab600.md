# Run log — t_333ab600 (release-docs, E3.7)

**Data:** 21/09/2026
**Responsável:** João Pedro G M Silva - PUC Goiás ADS - 20251012000740

## Escopo executado

PR #40 (`docs/release-ciclo3`) — feat(release): E3.7 — release assinado
(.aab) e docs/CI-CD.md — mergeado em `main` (f42bec0, squash, 4/4 checks
verdes + GitGuardian).

- `.github/workflows/release-apk.yml`: tag `v*` → `bundleRelease` assinado
  (JDK 21; secrets `BRAINOUT_KEYSTORE_BASE64`, `BRAINOUT_KEYSTORE_PASSWORD`,
  `BRAINOUT_KEY_ALIAS`, `BRAINOUT_KEY_PASSWORD`); artifact do `.aab`.
- `app/build.gradle.kts`: `signingConfig` release via env vars; sem elas,
  build não-assinado (CI comum não quebra).
- `docs/CI-CD.md`: tabela dos 4 secrets, geração do keystore
  (`keytool -genkeypair`), corte de release, verificação do `.aab`
  (`jarsigner`/`apksigner`).
- `docs/ROADMAP.md`: E3.7 marcado concluído.

## Validação de ponta a ponta

| Run | Tag | Resultado |
|-----|-----|-----------|
| 35597593408 | v0.3.0-ciclo3 | failure — `validateSigningRelease`: keystore escrito na raiz, mas `file()` do módulo `:app` resolve contra `app/` |
| 35598981118 | v0.3.1-ciclo3 (1ª) | failure — `signReleaseBundle`: `Get Key failed: Given final block not properly padded` (KEY_PASSWORD ≠ senha da chave em PKCS12) |
| 35599507321 | v0.3.1-ciclo3 (2ª) | **success** — artifact `brainout-release-aab-v0.3.1-ciclo3` (4,2 MB) |

### Correções aplicadas durante a validação

- PR #43 (d3763ea): keystore decodificado em `$RUNNER_TEMP/brainout-release.jks`
  (path absoluto) em vez de `keystore.jks` relativo — mergeado, 4/4 verde.
- Secrets `BRAINOUT_*` cadastradas no repositório (o dono é o próprio usuário;
  nenhuma credencial versionada).
- `BRAINOUT_KEY_PASSWORD` redefinida com o mesmo valor de
  `BRAINOUT_KEY_PASSWORD` do keystore (PKCS12 exige).

### Verificação do `.aab`

Download do artifact `brainout-release-aab-v0.3.1-ciclo3` (app-release.aab,
4.290.491 bytes):

```
jarsigner -verify app-release.aab  ->  jar verified.
keytool -printcert -jarfile app-release.aab
  Owner: CN=Joao Pedro G M Silva, OU=ADS, O=PUC Goias ...
  SHA256: 43:D8:AF:C9:B2:E6:6A:81:73:2D:44:7F:03:17:AD:48:65:E7:F9:DA:1A:95:AB:B6:6D:65:BF:F2:E6:A1:78:1C
unzip -l: META-INF/BRAINOUT.SF, META-INF/BRAINOUT.RSA presentes (v1/JAR signature)
```

## Pós-validação

- PR #47 (fab45e9): dois novos sintomas reais registrados em
  `docs/CI-CD.md > Resolução de problemas` — mergeado.
- Tag final de referência: `v0.3.1-ciclo3` → d3763ea. A tag intermediária
  `v0.3.0-ciclo3` foi removida do remoto por ter sido cortada sobre o commit
  anterior à correção #43.
- Keystore físico + senhas: fora do repositório (cofre local do dono).