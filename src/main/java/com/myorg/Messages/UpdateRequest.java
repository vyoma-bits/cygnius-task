package com.myorg.Messages;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class UpdateRequest {

    private Session entity;
    private List<String> fieldsToUpdate;

    @JsonProperty("entity")
    public Session getEntity() {
        return entity;
    }

    public void setEntity(Session entity) {
        this.entity = entity;
    }

    @JsonProperty("fieldsToUpdate")
    public List<String> getFieldsToUpdate() {
        return fieldsToUpdate;
    }

    public void setFieldsToUpdate(List<String> fieldsToUpdate) {
        this.fieldsToUpdate = fieldsToUpdate;
    }
}
