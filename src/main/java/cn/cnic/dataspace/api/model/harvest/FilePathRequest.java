package cn.cnic.dataspace.api.model.harvest;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class FilePathRequest {

    @NotNull(message = "linkId is not null")
    private String linkId;

    private String password;

    private List<String> hashList;
}
