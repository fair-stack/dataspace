package cn.cnic.dataspace.api.model.open;

import lombok.Data;
import java.util.List;

@Data
public class AppAuthApi {

    public AppAuthApi() {
    }

    public AppAuthApi(String appKey, List<String> pathList) {
        this.appKey = appKey;
        this.pathList = pathList;
    }

    private String appKey;

    private List<String> pathList;
}
