# Contas locais independentes com nome de usuário

Decisão aprovada em 26/10/2026: substituir e-mail e papéis OWNER/MEMBER por contas pessoais com nome de usuário único, senha e acesso completo aos próprios dados. A unicidade distingue maiúsculas por escolha explícita do autor; Joao e joao são contas diferentes. UUID continua sendo a identidade estável e não depende do nome.

A separação vale também para detalhes, escritas por ID, backup e lembretes. O backup passa a ser pessoal: sua restauração importa dados para a conta conectada, preserva suas credenciais e não modifica outras contas. Isso substitui a política anterior de exportar/restaurar o aparelho inteiro e usar a senha do arquivo como senha das contas importadas.

Bases anteriores são preservadas pela migração 5→6. Seus nomes tornam-se conta_1, conta_2… em ordem de criação e UUID, com aviso de correspondência e senhas preservadas. Arquivos v1 continuam legíveis mediante seleção de uma conta; arquivos novos usam v2 e recebem novos IDs ao importar. A senha do backup protege somente o arquivo, permitindo transferir dados entre instalações sem transportar hashes ou material do Keystore.
