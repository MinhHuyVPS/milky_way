package milky_way.utils.enums;

import lombok.Getter;

@Getter
public enum UserRole {
    ADMIN("ADMIN"),
    SELLER("SELLER"),
    USER("USER");

    private final String value;

    UserRole(String value){
        this.value = value;
    }

}
