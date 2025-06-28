package com.myorg.Messages;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * This class is used as template class for updation request
 * @param <T>
 */
public class UpdateRequest<T> {

    private T entity;
    private List<String> fieldsToUpdate;

    @JsonProperty("entity")
    public T getEntity() {
        return entity;
    }

    public void setEntity(T entity) {
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