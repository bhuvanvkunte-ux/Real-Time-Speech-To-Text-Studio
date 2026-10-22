package com.procucev.transcription.dto;

import java.util.List;

public class BulkDeleteRequest {

    private List<Long> ids;

    public BulkDeleteRequest() {}

    public BulkDeleteRequest(List<Long> ids) {
        this.ids = ids;
    }

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}
