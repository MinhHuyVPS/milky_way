package milky_way.utils.enums;

import lombok.Getter;

@Getter
public enum ShippingStatus {
    ALL("ALL"),
    TRANSPORT("TRANSPORT"),
    WAITING_FOR_PAYMENT("WAITING_FOR_PAYMENT"),
    WAITING_FOR_DELIVERY("WAITING_FOR_DELIVERY"),
    COMPLETE("COMPLETE"),
    CANCELED("CANCELED"),
    RETURN_GOODS("RETURN_GOODS"),
    REFUND("REFUND"),
    WAIT_FOR_CONFIRMATION("WAIT_FOR_CONFIRMATION");

    private final String value;

    ShippingStatus(String value){
        this.value = value;
    }
}
