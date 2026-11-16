package ru.adept.core.client;

import ru.adept.core.artifact.GaussGunItem;
import ru.adept.core.artifact.GaussLook;

/** Рендер гаусс-пушки: общий рендер артефакта по сегментам с её моделью, текстурой и анимациями. */
public class GaussGunRenderer extends ArtifactItemRenderer<GaussGunItem> {
    public GaussGunRenderer() {
        super("item/gauss_gun", "gauss_gun", "gauss_gun", GaussLook.SEGMENTS);
    }
}
