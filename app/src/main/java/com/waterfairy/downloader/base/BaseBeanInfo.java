package com.waterfairy.downloader.base;

import java.util.HashMap;

/**
 * @author water_fairy
 * @email 995637517@qq.com
 * @date 2019/4/4 15:44
 * @info:
 */
public class BaseBeanInfo {

    public static final int ERROR_CODE = 10001;//执行retrofit的时候出的异常

    public static final int STATE_WAITING = 0;
    public static final int STATE_START = 1;
    public static final int STATE_LOADING = 2;
    public static final int STATE_PAUSED = 3;
    public static final int STATE_SUCCESS = 4;
    public static final int STATE_ERROR = 5;


    private Object object;
    private String errMsg;
    private int state;
    private boolean isPost=false;
    private HashMap<String, String> paramsHashMap;

    public HashMap<String, String> getParamsHashMap() {
        return paramsHashMap;
    }

    public void setParamsHashMap(HashMap<String, String> paramsHashMap) {
        this.paramsHashMap = paramsHashMap;
    }

    public boolean isPost() {
        return isPost;
    }

    public void setPost(boolean post) {
        isPost = post;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    /**
     * 上传后的文件地址 / 下载地址
     *
     * @return
     */
    protected String url;
    /**
     * 上传地址
     */
    protected String uploadUrl;
    /**
     * 本地地址
     *
     * @return
     */
    protected String filePath;

    /**
     * 总大小
     */
    protected long totalLength;

    /**
     * 当前大小
     */
    protected long currentLength;


    public String getUrl() {
        return url;
    }

    public BaseBeanInfo setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public BaseBeanInfo setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
        return this;
    }

    public String getFilePath() {
        return filePath;
    }

    public BaseBeanInfo setFilePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    public long getTotalLength() {
        return totalLength;
    }

    public BaseBeanInfo setTotalLength(long totalLength) {
        if (this.totalLength == 0)
            this.totalLength = totalLength;
        return this;
    }

    public long getCurrentLength() {
        return currentLength;
    }

    public BaseBeanInfo setCurrentLength(long currentLength) {
        this.currentLength = currentLength;
        return this;
    }

    public BaseBeanInfo setState(int state) {
        this.state = state;
        return this;
    }

    public int getState() {
        return state;
    }
}
