package com.waterfairy.downloader.base;


import com.waterfairy.downloader.base.BaseBeanInfo;

/**
 * @author water_fairy
 * @email 995637517@qq.com
 * @date 2019/3/26 14:40
 * @info:
 */
public interface ProgressListener {
    void onProgressing(BaseBeanInfo beanInfo, long total, long current);
}
