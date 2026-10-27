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
  "text": { "public": ["ключ"], "streamer": ["ключ"] }
}
```

  `page`, `repeat`, `popup` можно не писать (по умолчанию `entries`, `once`, нет). Вместо `public`/`streamer` можно `common`.
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

Остальные типы в данных (опоры, герои, руины, финал и т. п.) ждут событий следующих срезов; при загрузке они перечислены в логе.

## Субтитры и торговец

- Реплики торговца: `trader/TraderTalk.java`, тексты `adept_story.trader.*`.
- Появление: первый ритуал — свои реплики по ветке; первый ритуал на новом уровне узлов — веха (реплика и запись на странице «Торговец»); иначе случайное приветствие.
- Открытие счёта, особые продажи (`trophy`, `pillar_material`), прощание при уходе.

## Клиент

- Журнал: клавиша `J` (меняется в настройках, раздел «Сюжет») и кнопка «Ж» над окном инвентаря.
- Субтитры внизу экрана по очереди; всплывающие записи (вехи) — сверху; о новой записи — уведомление справа сверху.

## Отладка

`/story` (права 2): `journal list | reset | add <id>`, `trigger <тип> [k=v …]`, `trader arrive | ritual`, `subtitle <ключ>`.
