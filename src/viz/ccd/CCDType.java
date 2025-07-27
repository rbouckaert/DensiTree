package viz.ccd;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Enum class for choosing the CCD type for convergence criteria and tools.
 */
public enum CCDType {
    CCD0("CCD0") {
        @Override
        public AbstractCCD emptyCCDOfType(int numberOfLeaves) {
            return new CCD0(numberOfLeaves, false);
        }
    },
    CCD1("CCD1") {
        @Override
        public AbstractCCD emptyCCDOfType(int numberOfLeaves) {
            return new CCD1(numberOfLeaves, false);
        }
    },
    CCD2("CCD2") {
        @Override
        public AbstractCCD emptyCCDOfType(int numberOfLeaves) {
            return new CCD2(numberOfLeaves, false);
        }
    };

    String ccdType;

    CCDType(String description) {
        this.ccdType = description;
    }

    // Method to get the enum constant from a string value
    public static CCDType fromName(String name) {
        for (CCDType myEnum : CCDType.values()) {
            if (myEnum.ccdType.equalsIgnoreCase(name)) {
                return myEnum;
            }
        }

        if (name.equalsIgnoreCase("0")) {
            return CCD0;
        } else if (name.equalsIgnoreCase("1")) {
            return CCD1;
        } else if (name.equalsIgnoreCase("2")) {
            return CCD2;
        }

        throw new IllegalArgumentException("No CCD type with name '" + name + "' found." +
                " Try " + Arrays.stream(values()).map(x -> x.ccdType).collect(Collectors.joining(", ")));
    }

    // Method to get the string value of the enum
    public String getStringValue() {
        return ccdType;
    }

    @Override
    public String toString() {
        return ccdType;
    }

    public abstract AbstractCCD emptyCCDOfType(int numberOfLeaves);
}

