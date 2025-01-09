package milky_way.utils.enums;

import lombok.Getter;

@Getter
public enum DiscountType {
    PERCENTAGE("PERCENTAGE"),
    FIXED_AMOUNT("FIXED_AMOUNT");

    private final String value;

    DiscountType(String value){
        this.value = value;
    }
}
