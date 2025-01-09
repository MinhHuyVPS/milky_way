package milky_way.utils.enums;

import lombok.Getter;

@Getter
public enum NotificationType {
    SHIPPING("SHIPPING"),
    PROMOTION("PROMOTION"),
    REPORT("REPORT");
    private final String value;

    NotificationType(String value) {
        this.value = value;
    }
}
