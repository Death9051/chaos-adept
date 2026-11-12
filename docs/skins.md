# Скины героев

Дополнение к «Списку модов». Дата: 10 ноября 2026. Скины героев — из открытой базы SkinsDB (skinsdb.terraqueststudios.net, база мода skinsdb для Luanti/Minetest): у каждого скина в базе указаны автор и лицензия. Берём только лицензии без запрета коммерческого использования: CC0, CC BY, CC BY-SA. Сайт открывается из России, API отдаёт базу целиком (`/api/v1/content?client=script&page=N&per_page=1000`).

Файлы: `core/src/main/resources/assets/adept_core/textures/entity/hero/<класс>_<n>.png`, по три на класс; вариант постоянен для героя (по UUID). Изменения: исходники 64×32 переведены в 64×64 так же, как это делает игра со старыми скинами (левые рука и нога — зеркальные копии правых), рисунок не менялся. Изменённые файлы распространяются под той же лицензией, что исходник.

Лицензию указывает автор при загрузке; первоисточник скина база не проверяет. Если автор или правообладатель попросит убрать скин, заменяем его.

| Файл | Скин в базе (id) | Автор | Лицензия |
|---|---|---|---|
| warrior_1 | character_CTF_blue_knight (2035) | Sam_0 | CC BY 3.0 |
| warrior_2 | character_CTF_red_knight (2036) | Sam_0 | CC BY 4.0 |
| warrior_3 | Dumb crusader (1415) | PiccoPaxxo | CC0 1.0 |
| archer_1 | Farmer1 (1999) | sdzen | CC BY-SA 3.0 |
| archer_2 | Farmer5 (2003) | sdzen | CC BY-SA 3.0 |
| archer_3 | Farmer6 (2004) | sdzen | CC BY-SA 3.0 |
| tracker_1 | Adventurer (389) | XSuperSaintX | CC BY 3.0 |
| tracker_2 | Camouflaged wizard (696) | nm0i | CC BY-SA 4.0 |
| tracker_3 | Woodcutter (732) | Watola | CC BY-SA 3.0 |
| commander_1 | King Gerold (1434) | Extex | CC BY-SA 3.0 |
| commander_2 | King of war (2313) | Miche | CC BY-SA 4.0 |
| commander_3 | Golden Knight (225) | Nero3605 | CC BY-SA 3.0 |
| mage_1 | The Green Wizard (1435) | Extex | CC BY-SA 3.0 |
| mage_2 | mage (462) | Ginsu23 | CC BY 4.0 |
| mage_3 | Th Blue Sorcerer (300) | Nero3605 | CC BY-SA 3.0 |
| healer_1 | priest? (1998) | sdzen | CC BY-SA 3.0 |
| healer_2 | character_CTF_red_medic (2037) | Sam_0 | CC BY 4.0 |
| healer_3 | Angel (878) | nelly | CC BY-SA 3.0 |

Тексты лицензий: CC0 1.0 — creativecommons.org/publicdomain/zero/1.0; CC BY 3.0 / 4.0 — creativecommons.org/licenses/by/3.0, /by/4.0; CC BY-SA 3.0 / 4.0 — creativecommons.org/licenses/by-sa/3.0, /by-sa/4.0. Этот список — указание авторства; при публикации сборки положить его в описание или в файл рядом с модом.

## Правила

- Новые скины — только с лицензией без NC и ND, автор и лицензия — сюда же.
- Не брать скины с чужими персонажами (игры, фильмы, сериалы), даже с открытой лицензией.
- Имя класса в файле — `HeroClass.id()`; геймтест `heroSkinsForEveryClass` проверяет, что у каждого класса есть три скина.
