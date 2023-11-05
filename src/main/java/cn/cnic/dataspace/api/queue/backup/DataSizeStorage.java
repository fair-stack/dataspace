package cn.cnic.dataspace.api.queue.backup;

import lombok.Data;

@Data
public class DataSizeStorage {

    private long total;

    public DataSizeStorage(long total) {
        this.total = total;
    }

    public long getTotal() {
        return total;
    }

    public void addSize(long size) {
        this.total += size;
    }

    public void destroy() {
        this.total = 0;
    }
}
