package com.kmsg.digitaldisplay.data;

/**
 * Created by ADMIN on 27-Nov-17.
 * Content model
 */

public class Content {

    private int contentId;
    private String contentName;
    private String pathContent;
    private String pathLocalContent;
    private String pathTempContent;

    public Content(int contentId, String pathLocalContent) {
        this.contentId = contentId;
        this.pathLocalContent = pathLocalContent;
    }

    public Content(int contentId, String contentName, String pathContent) {
        this.contentId = contentId;
        this.contentName = contentName;
        this.pathContent = pathContent;
    }

    public Content(int contentId, String contentName, String pathContent, String pathTempContent) {
        this.contentId = contentId;
        this.contentName = contentName;
        this.pathContent = pathContent;
        this.pathTempContent = pathTempContent;
    }

    public int getContentId() {
        return contentId;
    }

    public String getContentName() {
        return contentName;
    }

    public String getPathContent() {
        return pathContent;
    }

    public String getPathLocalContent() {
        return pathLocalContent;
    }

    public void setPathLocalContent(String pathLocalContent) {
        this.pathLocalContent = pathLocalContent;
    }

    public String getPathTempContent() {
        return pathTempContent;
    }

    public void setPathTempContent(String pathTempContent) {
        this.pathTempContent = pathTempContent;
    }
}
