package ru.adept.core.component;

import java.util.List;

/**
 * Семейства компонентов («Материалы и расходники», «Компоненты»).
 * Природа семейства решает, есть ли у него магическая и техническая вариации.
 */
public enum ComponentFamily {
    WIRING("wiring", Nature.TECH),
    COIL("coil", Nature.BOTH),
    CAPACITOR("capacitor", Nature.BOTH),
    FRAME("frame", Nature.TECH),
    RADIATOR("radiator", Nature.TECH),
    NOZZLE("nozzle", Nature.TECH),
    ACTUATOR("actuator", Nature.BOTH),
    SPRING("spring", Nature.BOTH),
    LENS("lens", Nature.BOTH),
    SENSOR("sensor", Nature.BOTH),
    EMITTER("emitter", Nature.BOTH),
    MANA_CIRCUIT("mana_circuit", Nature.MAGIC),
    MATRIX("matrix", Nature.MAGIC),
    CONTROL_BOARD("control_board", Nature.BOTH);

    /** Природа семейства: только магическое, только техническое или с двумя вариациями. */
    public enum Nature {
        MAGIC, TECH, BOTH
    }

    public static final int MAX_RANK = 3;

    private final String id;
    private final Nature nature;

    ComponentFamily(String id, Nature nature) {
        this.id = id;
        this.nature = nature;
    }

    public String id() {
        return id;
    }

    public Nature nature() {
        return nature;
    }

    /** Вариации, в которых существует семейство. У семейств одной природы — одна, своя. */
    public List<ComponentVariant> variants() {
        return switch (nature) {
            case MAGIC -> List.of(ComponentVariant.MAGIC);
            case TECH -> List.of(ComponentVariant.TECH);
            case BOTH -> List.of(ComponentVariant.MAGIC, ComponentVariant.TECH);
        };
    }

    /** Id предмета: {@code coil_2_magic} у семейств с вариациями, {@code wiring_2} у остальных. */
    public String itemId(int rank, ComponentVariant variant) {
        return nature == Nature.BOTH ? id + "_" + rank + "_" + variant.id() : id + "_" + rank;
    }

    public static ComponentFamily byId(String id) {
        for (ComponentFamily f : values()) {
            if (f.id.equals(id)) {
                return f;
            }
        }
        return null;
    }
}
