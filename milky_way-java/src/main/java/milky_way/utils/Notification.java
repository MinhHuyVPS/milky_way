package milky_way.utils;

public enum Notification {

    REGISTER("Default role for new users"),
    INVALID_CREDENTIALS("Invalid credentials!"),
    LOGOUT("Logout successful!"),
    RESET_PASSWORD("Reset password email sent"),
    RESET_PASSWORD_SUCCESSFUL("Password reset successfully."),
    LOGIN("Login successful!");
    private final String value;

    Notification(String value){
        this.value = value;
    }

    public String getValue(){
        return value;
    }
}
