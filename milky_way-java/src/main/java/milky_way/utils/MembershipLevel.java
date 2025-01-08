package milky_way.utils;

import lombok.Getter;

@Getter
public enum MembershipLevel {
    SILVER("SILVER"),
    GOLD("GOLD"),
    PLATINUM("PLATINUM");
    private final String value;

    MembershipLevel(String value) {
        this.value = value;
    }
}
