package milky_way.utils;

import lombok.Getter;
@Getter
public enum Permissions {
    SELLER_READ("SELLER_READ"),
    SELLER_WRITE("SELLER_WRITE"),
    SELLER_DELETE("SELLER_DELETE"),
    USER_READ("USER_READ"),
    USER_WRITE("USER_WRITE"),
    USER_DELETE("USER_DELETE"),
    ADMIN_READ("ADMIN_READ"),
    ADMIN_WRITE("ADMIN_WRITE"),
    ADMIN_DELETE("ADMIN_DELETE");

    private final String value;

    Permissions(String value) {
        this.value = value;
    }
}
