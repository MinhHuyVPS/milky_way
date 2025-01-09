package milky_way.utils.enums;

import lombok.Getter;

@Getter
public enum NotificationStatus {
    SENT("SENT"),
    SEEN("SEEN");
    private final String value;

    NotificationStatus(String value) {
        this.value = value;
    }
}
