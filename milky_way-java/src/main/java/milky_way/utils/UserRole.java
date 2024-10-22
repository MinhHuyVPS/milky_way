package milky_way.utils;

public enum UserRole {
    ADMIN("admin"),
    SELLER("seller"),
    USER("user");

    private final String value;

    UserRole(String value){
        this.value = value;
    }

    public String getValue(){
        return value;
    }
}
