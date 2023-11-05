package cn.cnic.dataspace.api.model.statistics;

import lombok.Data;

@Data
public class DataIn {

    private long webData;

    private long fairLinkData;

    private long ftpData;

    private long webDavData;
}
