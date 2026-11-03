package com.ecampus.dto.master;

public class BatchCreateForm {

    private Long bchid;
    private Long bchprgid;
    private String bchname;
    private Long bchcapacity;
    private String bchinstcode;
    private Long schemeId;
    private Long splid;

    public BatchCreateForm() {}

    public Long getBchid() {
        return bchid;
    }

    public void setBchid(Long bchid) {
        this.bchid = bchid;
    }

    public Long getBchprgid() {
        return bchprgid;
    }

    public void setBchprgid(Long bchprgid) {
        this.bchprgid = bchprgid;
    }

    public String getBchname() {
        return bchname;
    }

    public void setBchname(String bchname) {
        this.bchname = bchname;
    }

    public Long getBchcapacity() {
        return bchcapacity;
    }

    public void setBchcapacity(Long bchcapacity) {
        this.bchcapacity = bchcapacity;
    }

    public String getBchinstcode() {
        return bchinstcode;
    }

    public void setBchinstcode(String bchinstcode) {
        this.bchinstcode = bchinstcode;
    }

    public Long getSchemeId() {
        return schemeId;
    }

    public void setSchemeId(Long schemeId) {
        this.schemeId = schemeId;
    }

    public Long getSplid() {
        return splid;
    }

    public void setSplid(Long splid) {
        this.splid = splid;
    }
}

