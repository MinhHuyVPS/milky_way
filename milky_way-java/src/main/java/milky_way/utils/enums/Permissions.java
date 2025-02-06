package milky_way.utils.enums;

import lombok.Getter;
@Getter
public enum Permissions {
    SHOP_READ("SHOP_READ"),
    SHOP_WRITE("SHOP_WRITE"),
    SHOP_DELETE("SHOP_DELETE"),
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
