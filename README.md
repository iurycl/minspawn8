# MinSpawn8

Mod NeoForge (1.21.1) server-side que:

1. Torna a distância mínima de spawn natural de mobs configurável em runtime, **separada para mobs hostis e não-hostis** (padrão: 8 blocos para ambos, era 24 no vanilla).
2. Adiciona um item **Pena de Seleção de Região** (`minspawn8:spawn_feather`, visual de pena de galinha) para marcar regiões clicando com o botão direito em dois blocos (estilo Flan). Cada região tem uma taxa de spawn (%) configurável, **separada para hostis e não-hostis**, e automaticamente cria uma "zona de compensação" ao redor.

## Build

```
./gradlew build
```

O jar final fica em `build/libs/minspawn8-2.0.1.jar`. Copie para a pasta `mods` do servidor.

## Testar localmente

```
./gradlew runServer
```

Sobe um servidor de teste em `run/` usando o mod compilado (ctrl+C para parar).

## Como usar

- Craft: 1 pena normal em cima, 2 gravetos embaixo (igual uma pá), no formato:
  ```
  F
  #
  #
  ```
  (F = pena, # = graveto). Qualquer jogador pode craftar, não precisa ser operador.
- Ou dê diretamente com `/give @s minspawn8:spawn_feather`.
- Clique com o direito em um bloco → define o ponto 1.
- Clique com o direito em outro bloco → define o ponto 2 e cria a região (mensagem no chat mostra o ID).
- Ajuste a taxa de spawn dessa região com:
  - `/minspawn8 region percent <id> hostile <percentual>` (mobs hostis, ex: zumbis, esqueletos, creepers)
  - `/minspawn8 region percent <id> nonhostile <percentual>` (mobs não-hostis, ex: vacas, porcos, galinhas)

## Comandos (requer permissão de operador, nível 2)

- `/minspawn8` ou `/minspawn8 help` — mostra a lista de comandos disponíveis.
- `/minspawn8 distance` — mostra a distância mínima de spawn atual (hostil e não-hostil).
- `/minspawn8 distance hostile <blocos>` — define a distância mínima de spawn para mobs hostis (0–128).
- `/minspawn8 distance nonhostile <blocos>` — define a distância mínima de spawn para mobs não-hostis (0–128).
- `/minspawn8 region list` — lista todas as regiões marcadas (com os dois percentuais).
- `/minspawn8 region percent <id>` — mostra os percentuais atuais de uma região.
- `/minspawn8 region percent <id> hostile|nonhostile <percentual>` — define a taxa de spawn da região para aquele grupo (100 = vanilla).
- `/minspawn8 region remove <id>` — remove uma região.

## Como funciona a zona de compensação

Ao marcar uma região, além da área selecionada (zona interna), o mod cria automaticamente uma **zona de compensação** ao redor — com a mesma largura e profundidade (em blocos) da própria seleção — onde o efeito é espelhado em torno de 100%:

- Se a região aumenta o spawn em **+50%** (percentual 150%) por dentro, a zona ao redor **diminui 50%** (percentual efetivo 50%).
- Se a região diminui o spawn em **-50%** (percentual 50%) por dentro, a zona ao redor **aumenta 50%** (percentual efetivo 150%).
- O percentual padrão é 100% (sem efeito, zona ao redor também fica neutra).
- O efeito nunca fica negativo (mínimo 0%, sem spawns).

Exemplo: uma seleção de 20x20 blocos cria uma zona de compensação que se estende mais 20 blocos para cada lado (norte, sul, leste, oeste) ao redor dela.

## Observações técnicas

- Configuração persistida em `config/minspawn8.json` (distâncias + lista de regiões), sobrevive a reinícios do servidor.
- "Hostil" = categoria `MONSTER` do Minecraft (zumbis, esqueletos, aranhas, creepers, etc). "Não-hostil" = todas as outras categorias de spawn natural (animais, criaturas aquáticas, ambiente).
- Os efeitos de região e da zona de compensação são aplicados por **chunk** (coluna 16x16), já que o `NaturalSpawner` do Minecraft processa spawns por chunk — a altura (Y) da sua seleção não restringe onde dentro do chunk o mob nasce, só o X/Z importa.
- Se duas regiões se sobrepõem na mesma coluna, vale o efeito de maior intensidade (o que mais se desvia de 100%) entre as zonas internas; só se nenhuma zona interna cobrir a coluna é que as zonas de compensação são consideradas.
