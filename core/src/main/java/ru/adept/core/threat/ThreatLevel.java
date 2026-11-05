package ru.adept.core.threat;

import ru.adept.core.config.BalanceConfig;

/** Ступени угрозы мира («Мобы», «Герои и набеги»). Пороги — в конфиге {@code threat}. */
public enum ThreatLevel {
    /** 0–24: набегов нет. */
    CALM,
    /** 25–49: разведка — одиночные следопыты или пары. */
    SCOUTS,
    /** 50–74: отряды из 3–5 героев. */
    SQUADS,
    /** 75–100: крупные отряды с командиром, часть верхом. */
    WARBANDS;

    public static ThreatLevel of(int threat) {
        if (threat >= BalanceConfig.THREAT_WARBANDS_AT.get()) {
            return WARBANDS;
        }
        if (threat >= BalanceConfig.THREAT_SQUADS_AT.get()) {
            return SQUADS;
        }
        if (threat >= BalanceConfig.THREAT_SCOUTS_AT.get()) {
            return SCOUTS;
        }
        return CALM;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
