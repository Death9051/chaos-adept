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
- **clienttest** — клиент под виртуальным экраном создаёт мир, 200 тиков в нём и закрывается. Тест пройден, если в логе есть `ADEPT_CLIENTTEST_OK`.
- **logs** — логи servertest и clienttest каждого прогона в ветке `ci-logs`, папка `runs/<номер>-<коммит>/`, итог в `result.txt`. Последний прогон указан в `latest.txt`. Хранятся 30 прогонов.
- **fullpack** — прогон со всем модпаком, добавим вместе со сборкой модпака.

## Что где

- Флаги мира (общие для модов): `ru.adept.core.api.WorldFlags`.
- Ветка сюжета: `config/adept_story-common.toml`, параметр `branch` (`PUBLIC` по умолчанию). Записывается в мир при первом запуске.
- Анимации тела игрока для катсцен: `story/src/main/resources/assets/adept_story/player_animations/*.json`, экспорт из Blockbench в формате GeckoLib. Библиотека — Player Animation Library.
