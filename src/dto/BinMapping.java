package dto;

public class BinMapping {
    public String name;
    public Long rangeFrom;
    public Long rangeTo;
    public String type;
    public String country;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getRangeFrom() {
        return rangeFrom;
    }

    public void setRangeFrom(Long rangeFrom) {
        this.rangeFrom = rangeFrom;
    }

    public Long getRangeTo() {
        return rangeTo;
    }

    public void setRangeTo(Long rangeTo) {
        this.rangeTo = rangeTo;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
