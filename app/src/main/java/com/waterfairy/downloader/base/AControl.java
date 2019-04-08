package com.waterfairy.downloader.base;

import android.graphics.drawable.Icon;

/**
 * @author water_fairy
 * @email 995637517@qq.com
 * @date 2019/4/4 16:08
 * @info:
 */
public abstract class AControl implements IControl {
    protected BaseBeanInfo beanInfo;


    public BaseBeanInfo getBeanInfo() {
        return beanInfo;
    }

    public void setBeanInfo(BaseBeanInfo beanInfo) {
        this.beanInfo = beanInfo;
    }
}
