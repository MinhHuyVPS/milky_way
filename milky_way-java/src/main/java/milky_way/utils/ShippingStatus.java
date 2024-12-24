package milky_way.utils;

import lombok.Getter;

@Getter
public enum ShippingStatus {
    IN_TRANSIT("IN_TRANSIT"),
    DELIVERED("DELIVERED"),
    CANCELLED("CANCELLED");

    private final String value;

    ShippingStatus(String value){
        this.value = value;
    }
}
