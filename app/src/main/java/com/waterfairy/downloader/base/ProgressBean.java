package com.waterfairy.downloader.base;

/**
 * @author water_fairy
 * @email 995637517@qq.com
 * @date 2019/4/4 16:44
 * @info:
 */
public class ProgressBean {
    private int state;
    BaseBeanInfo beanInfo;

    public static final int STATE_START = 1;
    public static final int STATE_RESULT = 2;
    public static final int STATE_PAUSED = 3;//暂停
    public static final int STATE_PROGRESS = 4;
    private int resultCode;

    private String resultData;

    private long total;
    private long current;

    public ProgressBean(int state, BaseBeanInfo beanInfo) {
        this.state = state;
        this.beanInfo = beanInfo;

    }

    public int getResultCode() {
        return resultCode;
    }

    public ProgressBean setResultCode(int resultCode) {
        this.resultCode = resultCode;
        return this;
    }

    public int getState() {
        return state;
    }

    public ProgressBean setState(int state) {
        this.state = state;
        return this;
    }

    public BaseBeanInfo getBeanInfo() {
        return beanInfo;
    }

    public ProgressBean setBeanInfo(BaseBeanInfo beanInfo) {
        this.beanInfo = beanInfo;
        return this;
    }

    public String getResultData() {
        return resultData;
    }

    public ProgressBean setResultData(String resultData) {
        this.resultData = resultData;
        return this;
    }

    public long getTotal() {
        return total;
    }

    public ProgressBean setTotal(long total) {
        this.total = total;
        return this;
    }

    public long getCurrent() {
        return current;
    }

    public ProgressBean setCurrent(long current) {
        this.current = current;
        return this;
    }
}
