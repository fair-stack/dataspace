package cn.cnic.dataspace.api.model;

import lombok.Data;

@Data
public class IpInfo {

    /**
     * Country
     */
    private String country;

    /**
     * Region
     */
    private String region;

    /**
     * Province
     */
    private String province;

    /**
     * City
     */
    private String city;

    /**
     * Operator
     */
    private String isp;

    public String getCountry() {
        return country.equals("0") ? null : country;
    }

    public String getRegion() {
        return region.equals("0") ? null : region;
    }

    public String getProvince() {
        return province.equals("0") ? null : province;
    }

    public String getCity() {
        return city.equals("0") ? null : city;
    }

    public String getIsp() {
        return isp.equals("0") ? null : isp;
    }
}
