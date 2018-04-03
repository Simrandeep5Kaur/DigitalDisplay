package com.kmsg.digitaldisplay.data;

/**
 * Created by ADMIN on 06-Mar-18.
 */

public class ContentLog {
    private int ContentId;
    private String dtTm;

    public ContentLog(int contentId, String dtTm) {
        ContentId = contentId;
        this.dtTm = dtTm;
    }

    public int getContentId() {
        return ContentId;
    }

    public String getDtTm() {
        return dtTm;
    }
}
