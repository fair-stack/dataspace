package cn.cnic.dataspace.api.model.release.stemcells;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.List;

@Data
@Document(collection = "template_data")
public class SampleDo {

    @Id
    private String id;

    private String sampleId;

    private String templateId;

    private String orgId;

    private String iri;

    private String name;

    private List<SampleCore> sampleCoreList;

    // 0 to improve 1 incomplete
    private int type;

    private String annotation;

    private Date updateTime;

    private Date createTime;

    public SampleDo() {
    }

    public SampleDo(String sampleId, String templateId, String orgId, String iri, Date createTime) {
        this.sampleId = sampleId;
        this.templateId = templateId;
        this.orgId = orgId;
        this.iri = iri;
        this.createTime = createTime;
    }
}
