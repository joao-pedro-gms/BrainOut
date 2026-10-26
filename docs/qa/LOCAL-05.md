# LOCAL-05 — contas locais por nome de usuário

Validação em 26/10/2026, Windows com daemon Gradle em JDK 21, Android 17/API 37 no AVD Medium_Phone_API_37.0. A configuração local preexistente do daemon foi preservada após os comandos.

## Contrato exercitado

Cadastro com username, senha e confirmação; Joao e joao são contas distintas. Room v6 mantém hashes, UUIDs e relações da base antiga. Contas gerenciam somente seus próprios dados, inclusive por IDs recebidos de navegação/notificação. Logout preserva a marca de permissão de notificações.

Backup v2 contém uma conta. Restaurar remapeia IDs, conserva credenciais/sessão e demais contas; v1 permite selecionar a conta de origem. Testes cobrem cancelamento da seleção, falhas de arquivo/senha, seleção ausente e sessão alterada. A reconciliação de lembretes é protegida contra troca de sessão, dispensa notificações substituídas e repete falhas transitórias.

## Evidências

- 665 testes unitários sem falha nos oito módulos, incluindo concorrência de cadastro, isolamento entre contas, migração 5→6 e cadeia completa, backup em outra instalação, seleção e cancelamento do arquivo antigo.
- 12 testes instrumentados de dados/migração e 3 de login executados no Android 17, sem falha.
- Gates: ktlintCheck, detekt, lintDebug e Kover de domínio/dados (piso 60%); APK debug montado.
- Verificadores de tokens, roadmap, cobertura, manifesto local e conteúdo do APK de instrumentação; cinco testes dos scripts de release.
- Conferência visual no AVD: aviso da migração, sessão existente preservada, saída para login e cadastro com exatamente três campos, sem seletor de papel ou e-mail. Nenhuma conta ou dado existente foi apagado na conferência.

## Limites

Room e DataStore não compartilham transação: a substituição dos dados é atômica no Room, enquanto preferências são mantidas no DataStore. A validação não abrange todos os tamanhos de janela ou TalkBack em aparelho físico. Renomear/excluir contas, recuperar/trocar senha e compartilhar projetos continuam fora desta entrega.

## Capturas do AVD

[Login por nome de usuário](local05/login-en.png) · [Cadastro com confirmação](local05/register-en.png). Capturas em inglês, sem dados de contas.
