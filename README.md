# MUreset

Automação para MU Online (Windows) com UI em **Compose Desktop**. O app gerencia uma lista de personagens e executa o fluxo de `/reset` automaticamente quando o personagem atinge o nível alvo, distribuindo pontos de acordo com a configuração.

## Resumo rápido
- **UI**: cadastro de personagens, atributos, mapas e logs.
- **Bot**: procura janelas do jogo pelo título, lê stats, executa `/reset`, distribui pontos e verifica retorno ao modo hunt usando OpenCV.
- **Persistência**: salva a lista em `~/.mureset-characters.cfg`.

## Requisitos
- **Windows** (usa APIs Win32 via JNA).
- **JDK 17** (Gradle usa toolchain 17).

## Como rodar (dev)
```bash
./gradlew run
```

## Como gerar executável
```bash
./gradlew package
```
Saída típica do app:
- `build/compose/binaries/main/app/MUreset/MUreset.exe`

## Como usar
1. Abra o jogo com os personagens desejados.
2. No app, clique em **Add character** e preencha:
   - **Name**: deve bater com o nome do personagem no título da janela do jogo.
   - **Attributes** (Str/Agi/Sta/Ene/Cmd).
   - **Points/Reset**: pontos por reset do servidor.
   - **Solo level**: nível alvo para o modo solo após o reset.
   - **Warp map**: mapa de teleporte (ex.: Elbeland 2 / Elbeland 3).
   - **Overflow attribute**: atributo que recebe o restante dos pontos.
3. Clique **Start** para iniciar o bot.

## Como o bot funciona
- Procura janelas com prefixo:
  - `GlobalMuOnline - Powered by IGCN - Name: [NOME]`
- Extrai **Level**, **Master Level** e **Resets** do título da janela.
- Quando `Level == 400`, executa:
  - `/reset`
  - distribuição de pontos via `/addstr`, `/addagi`, `/addvit`, `/addene`, `/addcmd`
- Após reset, o bot:
  1. Teleporta para o **Warp map** configurado.
  2. Liga o Hunt com a tecla **Home**.
  3. Aguarda até atingir o **Solo level**.
  4. Retoma a rotina normal (party click + detecção de hunt).
- **Espera inatividade do usuário**: só roda se o PC estiver ocioso por 30s.

## Configuração salva
Arquivo: `~/.mureset-characters.cfg`
- Formato interno com `|` e `;` escapados, não precisa editar manualmente.

## Recursos visuais (templates)
Usados pelo OpenCV:
- `src/main/resources/play_button_template.png`
- `src/main/resources/pause_button_template.png`
- `src/main/resources/ok_dialog_template.png`
- `src/main/resources/elbeland2_template.png`
- `src/main/resources/elbeland3_template.png`

## Observações
- O bot usa `Robot` para mouse/teclado. Evite usar o PC enquanto ele estiver ativo.
- Se o título da janela do jogo mudar, a detecção precisa ser ajustada em `BotController.windowPrefix`.
