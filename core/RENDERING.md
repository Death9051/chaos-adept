# Рендер ядра и шейдеры (Iris)

В сборке есть Iris + Sodium, шейдерпак по умолчанию выключен. Всё своё ядро рисует так, чтобы оно
выглядело правильно и без шейдеров, и с шейдерпаком.

## Правила

- Свои core-шейдеры и пост-эффекты (`PostChain`, свои `ShaderInstance`) не используем. Если когда-нибудь
  понадобятся — только с запасным путём: при включённом шейдерпаке (`IrisApi.isShaderPackInUse()`)
  эффект выключается или заменяется ванильным.
- Рендер-типы — только ванильные, которые Iris перехватывает: `entityCutout`, `entityCutoutNoCull`,
  `entityTranslucent`, `itemEntityTranslucentCull`, блоки через `BlockRenderDispatcher`.
- Свечение (кристаллы, сегменты, глаза мобов) — отдельным слоем `RenderType.eyes` / `entityTranslucentEmissive`
  или glowmask GeckoLib; не через полную яркость основного слоя.
- Частицы — ванильные типы (`DustParticleOptions`, `ELECTRIC_SPARK` и т. п.) или свои с
  `ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT` / `OPAQUE`. Своих `ParticleRenderType` с особым состоянием рендера не заводим.
- Интерфейсы (экраны, строки на экране) рисуются через `GuiGraphics` — Iris их не трогает.
- Небо и туман базы-измерения — через `DimensionSpecialEffects` и параметры типа измерения, без своих шейдеров неба.

## Что сейчас в ядре

| Что | Как рисуется | Iris |
|---|---|---|
| Частицы Хаоса и разряды (`fx/ChaosParticles`) | ванильные `dust` и `electric_spark` | работает |
| Заглушки мобов (`client/StubEntityRenderer`) | блок через `BlockRenderDispatcher` | работает |
| Стол, машины, кристалл узла | обычные модели блоков | работает |
| Экраны стола, машин, перехода | `GuiGraphics` | работает |
| Гаусс-пушка (`client/GaussGunRenderer`) | GeckoLib, `entityCutoutNoCull`; свечение — второй проход с маской `gauss_gun_glowmask.png` и `RenderType.eyes` | в игре не проверено |
| Превью на столе (`client/GaussUpgradeScreen`), шкалы гаусса (`client/GaussHud`) | `GuiGraphics`, модель — обычный рендер предмета | в игре не проверено |

В игре с шейдерпаком не проверено — это за пользователем.
