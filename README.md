# chaos-adept

Свои моды второй сборки Minecraft: «Ядро» (`adept_core`) и «Сюжет» (`adept_story`).
Minecraft 1.21.1, NeoForge 21.1.264, Java 21, ModDevGradle 2.0.148.

## Устройство

```
core/    «Ядро» — все механики. Публичное API для других модов: ru.adept.core.api
story/   «Сюжет» — история, тексты, катсцены. Зависит от ядра той же версии.
.github/workflows/ci.yml   CI: build, servertest, clienttest, логи в ветку ci-logs
```

Оба мода собираются одной командой и выходят одной версией (`mod_version` в `gradle.properties`).

## Запуск в разработке

| Команда | Что делает |
|---|---|
| `./gradlew build` | собирает оба jar в `core/build/libs` и `story/build/libs` |
| `./gradlew :story:runClient` | клиент с обоими модами |
| `./gradlew :story:runServer` | сервер с обоими модами |
| `./gradlew :core:runClient` | клиент с одним ядром |
| `./gradlew :story:runGameTestServer` | все геймтесты обоих модов, затем выход (как в CI) |
| `./gradlew :story:runClientTest` | клиентский автотест: создать мир, побыть в нём, выйти (как в CI) |

IntelliJ IDEA: открыть папку как проект Gradle, запуски появятся сами.

## CI

- **build** — сборка обоих модов, jar выкладываются в артефакты прогона.
- **servertest** — сервер NeoForge прогоняет геймтесты (`@GameTestHolder` в пакетах `test`).
- **clienttest** — клиент под виртуальным экраном создаёт мир, 200 тиков в нём и закрывается, по ходу снимает скриншоты окна игры. Тест пройден, если в логе есть `ADEPT_CLIENTTEST_OK`. Если меню не открылось за 6 минут, тест сразу падает (экран ошибки модов и т. п. виден на скриншотах).
- **logs** — всё по прогону в ветке `ci-logs`: прогоны `main`, ручные и pull request из веток этого репозитория.
  - Папка прогона: `runs/<номер>-<ветка или prN>-<коммит>/`, итог — `result.txt`.
  - Внутри: `logs-build/build.log` (полный вывод сборки, ошибки компиляции), `logs-servertest/`, `logs-clienttest/logs/`, `logs-clienttest/ci-screenshots/*.jpg`.
  - Последний прогон: `latest.txt` (общий) и `latest/<ветка, / заменён на _>.txt`.
  - Хранятся 40 прогонов. Читать по прямой ссылке: `https://raw.githubusercontent.com/Death9051/chaos-adept/ci-logs/<путь>` (кэш до 5 минут; свежий список — через GitHub API).
- **fullpack** — прогон со всем модпаком, добавим вместе со сборкой модпака.

## Что где

- Флаги мира (общие для модов): `ru.adept.core.api.WorldFlags`.
- Прогресс мира (уровень узлов, угроза): `ru.adept.core.api.WorldProgress`.
- События ядра: `ru.adept.core.api.event`, публикуются на `NeoForge.EVENT_BUS` на сервере.
- Числа баланса: `config/adept_core-common.toml` (`ru.adept.core.config.BalanceConfig`), правятся без пересборки.
- Отладочные команды: `/adept ...` (права 2): `flag`, `tier`, `threat`, `artifact`; части среза добавляют свои ветки.
- Заглушки текстур до готовых моделей: `assets/adept_core/textures/{block,item,entity}/stub.png`.
- Тестовая сборка (сторонние моды только в запусках, не зависимости наших модов): `localRuntime` в `story/build.gradle`, версии в `gradle.properties`. Сейчас: Naturalist.
- Ветка сюжета: `config/adept_story-common.toml`, параметр `branch` (`PUBLIC` по умолчанию). Записывается в мир при первом запуске.
- Анимации тела игрока для катсцен: `story/src/main/resources/assets/adept_story/player_animations/*.json`, экспорт из Blockbench в формате GeckoLib. Библиотека — Player Animation Library.
