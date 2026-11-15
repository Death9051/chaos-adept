# «Сюжет» (`adept_story`)

## Журнал героя

- Описания записей: `src/main/resources/data/adept_story/journal/<раздел>/<имя>.json`, id записи — путь без `.json`.
- Тексты: `src/main/resources/assets/adept_story/lang/ru_ru.json`, ключи `adept_story.journal.<раздел>.<имя>.<ветка>[.<номер>]`.
- Формат описания:

```json
{
  "trigger": { "type": "node_absorbed", "level": 2, "first": "level" },
  "page": "entries | trader | epilogue",
  "repeat": "once | pool",
  "popup": true,
  "note": { "public": "ключ", "streamer": "ключ" },
  "text": { "public": ["ключ"], "streamer": ["ключ"] }
}
```

  `page`, `repeat`, `popup`, `note` можно не писать (по умолчанию `entries`, `once`, нет). Вместо `public`/`streamer` можно `common`.
  В `once` ключи — абзацы одной записи; в `pool` — варианты, берётся случайный неиспользованный, когда кончатся — записи нет.
- Запись срабатывает, если тип события совпал и все параметры `trigger` совпали с атрибутами события.
- Хранение: данные мира `adept_story_journal` (игра одиночная, журнал один на мир).

### Типы событий

| Тип | Откуда | Атрибуты |
|---|---|---|
| `first_dawn` | наступил второй игровой день | — |
| `item_obtained` | предмет в инвентаре (проверка раз в секунду) | `item` |
| `artifact_table_built` | стол артефактов скрафчен или поставлен | — |
| `biome_entered` | игрок в биоме с тегом | `biome_tag` |
| `artifact_pair_chosen` | `ArtifactEvent.PairChosen` | `category`, `variant` |
| `artifact_prototype_crafted` | `ArtifactEvent.PrototypeCrafted` | `artifact` |
| `artifact_segment_apex` | `ArtifactEvent.StepUnlocked` с ключом `…/apex` | `key` |
| `base_entered` | `BaseEvent.Entered` | — |
| `node_wave_cleared` | `NodeEvent.WaveCleared` | `level` |
| `node_player_died` | `NodeEvent.PlayerDied` | `level` |
| `node_cleansed` | `NodeEvent.Cleansed` | `level` |
| `node_absorbed` | `BiomeAbsorbedEvent` (уровень — от зачистки узла) | `level`, `first` = `game` / `level` / `none` |
| `trader_account_opened` | `TraderEvent.AccountOpened` | — |
| `ritual_done` | уход торговца после первого ритуала | `first` = `game` |
| `node_level_reached` | первый ритуал на новом уровне узлов (веха) | `level` |
| `threat_raised` | `WorldProgressEvent.ThreatLevelChanged`, ступень угрозы выросла | `level` |
| `node_tier_changed` | `WorldProgressEvent.NodeTierChanged`, мир перешёл на новый уровень узлов | `level` |
| `raid_won` | `RaidEvent.Ended`, отряд разгромлен | `level` (ступень угрозы) |
| `village_ruins_entered` | `VillageEvent.RuinsEntered` | — |
| `npc_met` | `VillageEvent.NpcNoticedPlayer` | `npc` (роль: `villager`, `elder`, позже `refugee`) |
| `soul_burned` | `SoulEvent.Burned`, горнило сожгло душу | `strength` (`normal`, `strong`, `guardian`) |
| `soul_core_obtained` | `SoulEvent.CoreObtained` | `strength` |
| `trader_deal` | `TraderEvent.Deal` | `currency` (`node_cluster`, `soul_core`) |
| `rune_engraved` | `RuneEvent.Engraved` | `material`, `item` (`needle`, `bolt`, `plate`), `has_word` (`true`/`false`) |
| `rune_erased` | `RuneEvent.Erased` | `material`, `item` |
| `focus_cut` | `FocusEvent.Cut` | `cut`, `crystal` |
| `focus_cracked` | `FocusEvent.Cracked` | `cut`, `crystal`, `segment` |
| `shield_broken` | `ShieldEvent.Broken` | — |

Котёл собран — `artifact_prototype_crafted` с `artifact` = `cauldron`.

Остальные типы в данных (опоры, герои, руины, финал и т. п.) ждут событий следующих срезов; при загрузке они перечислены в логе.

## Субтитры и торговец

- Реплики торговца: `trader/TraderTalk.java`, тексты `adept_story.trader.*`.
- Появление: первый ритуал — свои реплики по ветке; первый ритуал на новом уровне узлов — веха (реплика и запись на странице «Торговец»); иначе случайное приветствие.
- Открытие счёта, особые продажи (`trophy`, `pillar_material`), прощание при уходе.
- Ядра душ: первая сделка ядрами — реплика `soul_core` и условие на странице «Торговец» (`trader/soul_cores`). Первое появление после первой сожжённой души — реплика о котле `cauldron` вместо приветствия (отметка `trader:cauldron`).

## Реплики героев и жителей

- Герои: `talk/HeroTalk.java`, события `HeroEvent`. Ключи `adept_story.hero.<группа>.<событие>.<номер>`, группа — класс (`tracker`, `warrior`, `archer`, `commander`, `mage`, `healer`), мастер класса (`<класс>_master`), ранг (`apprentice` — строки о страхе, `veteran` — сдержанные, `master` — спокойные) или `common`. Набор на событие — строки класса, мастера класса (только у мастеров), ранга и общие. Число строк в группе — таблица `HeroTalk.LINES` (тест сверяет её с lang).
- События: `spotted`, `wounded`, `missed`, `player_lost`, `ally_fell`, `squad_retreating`, `ally_healed` (целитель вылечил союзника), `ally_revived` (целитель-мастер поднял павшего).
- Частота: шанс по рангу (ученик 0,7, ветеран 0,35, мастер 0,25), приказ отступать и подъём павшего — всегда. Откаты: герой 12 с, одно событие в отряде 10 с (на «союзник пал» отвечает один), общая пауза 3 с. Слышно в радиусе 32 блоков. Говорящий — класс или имя командира.
- Жители: `talk/VillageTalk.java`, событие `VillageEvent.NpcNoticedPlayer`. Ключи `adept_story.village.<роль>.<low|high>.<номер>`, высокая угроза — с 50. Пауза между репликами жителей 5 с (ядро и так шлёт не чаще раза в 30 с от NPC).
- Числа — раздел `talk` в `config/adept_story-common.toml`.

## Зачаток

- Запись пролога `prologue/seed` — по `item_obtained` с `item` = `adept_core:seed` (зачаток кладётся в инвентарь при первом входе). Текст по веткам: в публичной зачаток дал Альянс, в стримерской это часть снаряжения осколка.
- Название и подсказка предмета — ключи `item.adept_core.seed` и `item.adept_core.seed.desc` в lang сюжета (перекрывают ядро, как шаблон приказа). Подсказка общая для веток: lang не знает ветку мира.
- Строка руководства у рецепта стола — ключ `adept_story.guide.artifact_table.seed`.

## Пулы имён и приказ командира

- `data/adept_story/adept_names/villages.json` и `commanders.json` (`{"names": [...]}`), ядро берёт имена оттуда.
- Текст приказа — ключ `adept_core.commander_order.text` в lang сюжета (`%1$s` командир, `%2$s` деревня), перекрывает запасной текст ядра.

## Клиент

- Журнал — потёртая бумага (`textures/gui/sprites/journal/paper.png`, nine-slice), почерк записей — шрифт `adept_story:journal` (Bad Script), пометки на полях — `adept_story:journal_note` (Caveat), оба OFL, лицензии в `assets/adept_story/font/`. Записи — не квесты: без целей и галочек.
- Журнал: клавиша `J` (меняется в настройках, раздел «Сюжет») и кнопка «Ж» над окном инвентаря.
- Субтитры внизу экрана по очереди; всплывающие записи (вехи) — сверху; о новой записи — уведомление справа сверху.

## Отладка

`/story` (права 2): `journal list | reset | add <id>`, `trigger <тип> [k=v …]`, `trader arrive | ritual`, `subtitle <ключ>`, `talk hero <событие> <класс> <ранг>`, `talk village <роль> <угроза>` (реплика без шанса и откатов).
