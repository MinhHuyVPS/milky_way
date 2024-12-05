package milky_way.utils;

public enum UserRole {
    ADMIN("ADMIN"),
    SELLER("SELLER"),
    USER("USER");

    private final String value;

    UserRole(String value){
        this.value = value;
    }

    public String getValue(){
        return value;
    }
}
