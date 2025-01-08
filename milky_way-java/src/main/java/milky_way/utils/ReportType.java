package milky_way.utils;

import lombok.Getter;

@Getter
public enum ReportType {
    SALES("SALES"),
    INVENTORY("INVENTORY"),
    USERS("USERS");
    private final String value;

    ReportType(String value) {
        this.value = value;
    }
}
