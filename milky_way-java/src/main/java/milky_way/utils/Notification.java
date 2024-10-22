package milky_way.utils;

public enum Notification {

    REGISTER("Default role for new users"),
    INVALID_CREDENTIALS("Invalid credentials!"),
    LOGOUT("Logout successful!"),
    LOGIN("Login successful!");

    private final String value;

    Notification(String value){
        this.value = value;
    }

    public String getValue(){
        return value;
    }
}
