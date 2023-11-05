package cn.cnic.dataspace.api.model.space;

import lombok.Data;

@Data
public class SpaceCheck {

    private long capacity;

    private long actual;

    public synchronized void updateActual(long data) {
        if (data < 0) {
            if (this.actual == 0) {
                return;
            }
            this.actual = this.actual + data;
            if (this.actual < 0) {
                this.actual = 0;
            }
        } else {
            this.actual = this.actual + data;
        }
    }
}
