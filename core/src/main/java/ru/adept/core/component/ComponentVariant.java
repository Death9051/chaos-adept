package ru.adept.core.component;

/**
 * Вариация компонента: магическая (руны, кристаллы, заряд маной) или техническая
 * (сплав, заряд электричеством). Заменяет прежние обработки «заряженный / рунный».
 */
public enum ComponentVariant {
    MAGIC("magic"),
    TECH("tech");

    private final String id;

    ComponentVariant(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ComponentVariant byId(String id) {
        for (ComponentVariant v : values()) {
            if (v.id.equals(id)) {
                return v;
            }
        }
        return null;
    }
}
