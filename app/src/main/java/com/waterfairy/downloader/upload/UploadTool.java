package com.waterfairy.downloader.upload;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.waterfairy.downloader.base.BaseBeanInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author water_fairy
 * @email 995637517@qq.com
 * @date 2019/3/25 16:35
 * @info:
 */
public class UploadTool {

    public static final String STR_STATE_TAG = "state";
    public static final String STR_STATE_PROGRESS = "progress";

    private List<UploadTask> uploadTasks;//上传任务
    private OnUploadListener selfUploadListener;//监听
    private boolean callCancel;//是否调用取消

    private Context context;
    private OnUploadListener onUploadListener;

    public void setContext(Context context) {
        this.context = context;
    }

    public void setSelfUploadListener(OnUploadListener listener) {
        this.onUploadListener = listener;
        this.selfUploadListener = new OnUploadListener() {
            @Override
            public void onUploadStart(BaseBeanInfo mediaResBean) {
                sendIntent(mediaResBean, BaseBeanInfo.STATE_WAITING);
                if (onUploadListener != null) {
                    onUploadListener.onUploadStart(mediaResBean);
                }
            }

            @Override
            public String onUploadSuccess(BaseBeanInfo mediaResBean, String jsonResult) {
                sendIntent(mediaResBean, BaseBeanInfo.STATE_SUCCESS);
                if (onUploadListener != null) {
                    return onUploadListener.onUploadSuccess(mediaResBean, jsonResult);
                }
                return null;
            }

            @Override
            public void onUploadError(BaseBeanInfo mediaResBean) {
                sendIntent(mediaResBean, BaseBeanInfo.STATE_ERROR);
                if (onUploadListener != null) {
                    onUploadListener.onUploadError(mediaResBean);
                }
            }

            @Override
            public void onUploadAll() {
                if (onUploadListener != null) {
                    onUploadListener.onUploadAll();
                }
            }

            @Override
            public void onUploadPaused(BaseBeanInfo beanInfo) {
                sendIntent(beanInfo, BaseBeanInfo.STATE_PAUSED);
                if (onUploadListener != null) {
                    onUploadListener.onUploadPaused(beanInfo);
                }
            }

            @Override
            public void onUploading(BaseBeanInfo mediaResBean) {
                sendIntent(mediaResBean, BaseBeanInfo.STATE_LOADING, (int) (100 * mediaResBean.getCurrentLength() / (float) mediaResBean.getTotalLength()));
                if (onUploadListener != null) {
                    onUploadListener.onUploading(mediaResBean);
                }
            }
        };
    }

    /**
     * 发送广播
     *
     * @param mediaResBean
     * @param state
     */
    public void sendIntent(BaseBeanInfo mediaResBean, int state) {
        sendIntent(mediaResBean, state, 0);
    }

    /**
     * 发送广播
     *
     * @param mediaResBean
     * @param state
     * @param progress
     */
    public void sendIntent(BaseBeanInfo mediaResBean, int state, int progress) {
        if (mediaResBean != null) mediaResBean.setState(state);
        if (context == null || mediaResBean == null || TextUtils.isEmpty(mediaResBean.getPath()))
            return;
        Intent intent = new Intent();
        intent.setAction(mediaResBean.getPath());
        if (state == BaseBeanInfo.STATE_LOADING) {
            intent.putExtra(STR_STATE_PROGRESS, progress);
        }
        intent.putExtra(STR_STATE_TAG, state);
        context.sendBroadcast(intent);
    }

    private int maxNum;
    private int currentProgressNum;

    public UploadTool setMaxNum(int maxNum) {
        this.maxNum = maxNum;
        return this;
    }

    public void start() {
        /**
         *
         * 错误
         */
        startOrNext();
    }

    public void stop() {
        if (uploadTasks != null && uploadTasks.size() > 0) {
            for (int i = 0; i < uploadTasks.size(); i++) {
                UploadTask uploadTask = uploadTasks.get(i);
                uploadTask.cancel();
            }
        }
        currentProgressNum = 0;

    }

    public interface OnUploadListener {

        /**
         * 开始
         *
         * @param mediaResBean
         */
        void onUploadStart(BaseBeanInfo mediaResBean);

        /**
         * 上传进度
         *
         * @param mediaResBean
         */
        void onUploading(BaseBeanInfo mediaResBean);

        /**
         * 暂停
         *
         * @param beanInfo
         */
        void onUploadPaused(BaseBeanInfo beanInfo);

        /**
         * 上传成功  返回上传后的url
         *
         * @param mediaResBean
         * @param jsonResult
         * @return
         */
        String onUploadSuccess(BaseBeanInfo mediaResBean, String jsonResult);

        /**
         * 上传失败
         *
         * @param mediaResBean
         */
        void onUploadError(BaseBeanInfo mediaResBean);

        /**
         * 上传完成(包涵失败/暂停/成功)
         */
        void onUploadAll();
    }

    /**
     * 添加任务 list
     *
     * @param medalBeans
     */
    public UploadTool addUpload(List<BaseBeanInfo> medalBeans) {
        if (medalBeans != null) {
            for (int i = 0; i < medalBeans.size(); i++) {
                addUpload(medalBeans.get(i));
            }
        }
        return this;
    }

    /**
     * 添加任务
     *
     * @param beanInfo
     */
    public UploadTool addUpload(BaseBeanInfo beanInfo) {
        if (uploadTasks == null) uploadTasks = new ArrayList<>();
        if (TextUtils.isEmpty(beanInfo.getUrl()) && !TextUtils.isEmpty(beanInfo.getPath()) && new File(beanInfo.getPath()).exists()) {
            //可以下载
            //判断任务中是否已经存在
            UploadTask uploadTask = checkExist(beanInfo.getPath());
            if (uploadTask == null) {
                uploadTasks.add(new UploadTask(beanInfo).setOnUploadListener(getUploadOneListener()));
            } else {
                uploadTask.setCanAutoExecute(true);
            }
        } else {
            if (selfUploadListener != null)
                selfUploadListener.onUploadStart(beanInfo);
            if (!TextUtils.isEmpty(beanInfo.getUrl())) {
                beanInfo.setState(BaseBeanInfo.STATE_SUCCESS);
                //已经上传
                if (selfUploadListener != null && !callCancel)
                    selfUploadListener.onUploadSuccess(beanInfo, null);
            } else {
                beanInfo.setState(BaseBeanInfo.STATE_ERROR);
                //失败
                if (selfUploadListener != null && !callCancel)
                    selfUploadListener.onUploadError(beanInfo);
            }
        }
        return this;
    }


    /**
     * 判断是否与该任务 返该任务
     *
     * @param path
     * @return
     */
    private UploadTask checkExist(String path) {
        if (uploadTasks != null) {
            for (UploadTask task : uploadTasks) {
                if (TextUtils.equals(task.getBeanInfo().getPath(), path))
                    return task;
            }
        }
        return null;
    }

    public void pauseAll() {
        if (uploadTasks != null) {
            for (int i = 0; i < uploadTasks.size(); i++) {
                UploadTask uploadTask = uploadTasks.get(i);
                uploadTask.pause();
            }
        }
    }

    public void pause(String path) {
        if (uploadTasks != null) {
            for (int i = 0; i < uploadTasks.size(); i++) {
                UploadTask uploadTask = uploadTasks.get(i);
                if (TextUtils.equals(uploadTask.getBeanInfo().getPath(), path)) {
                    uploadTask.pause();
                    return;
                }
            }
        }
    }


    public static final int SIZE_TOTAL = 0;
    public static final int SIZE_SUCCESS = 1;
    public static final int SIZE_PAUSED = 2;
    public static final int SIZE_ERROR = 3;


    /**
     * 获取对应的数量
     *
     * @return
     */
    public int[] getSizes() {
        int totalSize = 0;
        int errorSize = 0;
        int successSize = 0;
        int pausedSize = 0;
        if (uploadTasks != null) {
            totalSize = uploadTasks.size();
            for (int i = 0; i < uploadTasks.size(); i++) {
                switch (uploadTasks.get(i).getBeanInfo().getState()) {
                    case BaseBeanInfo.STATE_ERROR:
                        errorSize++;
                        break;
                    case BaseBeanInfo.STATE_PAUSED:
                        pausedSize++;
                        break;
                    case BaseBeanInfo.STATE_SUCCESS:
                        successSize++;
                        break;
                }
            }
        }
        return new int[]{totalSize, successSize, pausedSize, errorSize};
    }

    /**
     * 开始下一个下载
     */
    private synchronized void startOrNext() {
        int[] sizes = getSizes();
        if (sizes[SIZE_TOTAL] > sizes[SIZE_SUCCESS] + sizes[SIZE_ERROR]) {
            //任务未执行完
            for (int i = 0; i < uploadTasks.size(); i++) {
                UploadTask task = uploadTasks.get(i);
                if (currentProgressNum >= maxNum) break;
                //排除以下条件
                if (task != null//任务不为空
                        && !task.isExecute()//任务未执行
                        && TextUtils.isEmpty(task.getBeanInfo().getUrl())//有上传链接
                        && task.getBeanInfo().getState() != BaseBeanInfo.STATE_ERROR//下载失败
                        && task.getBeanInfo().getState() != BaseBeanInfo.STATE_PAUSED//下载暂停
                        && task.getBeanInfo().getState() != BaseBeanInfo.STATE_SUCCESS) {//下载成功
                    if (task.getCanAutoExecute()) {
                        currentProgressNum++;
                        task.execute();
                        if (selfUploadListener != null && !callCancel)
                            selfUploadListener.onUploadStart(task.getBeanInfo());
                    }
                }
            }
        } else if (sizes[SIZE_TOTAL] != 0 && sizes[SIZE_TOTAL] == sizes[SIZE_SUCCESS] + sizes[SIZE_ERROR]) {
            //任务已经执行完毕
            if (onUploadListener != null)
                onUploadListener.onUploadAll();
        }
    }

    /**
     * 内部调用
     *
     * @return
     */
    private UploadTask.OnUploadListener getUploadOneListener() {
        return new UploadTask.OnUploadListener() {
            @Override
            public void onLoadStart(BaseBeanInfo beanInfo) {
                if (selfUploadListener != null && !callCancel)
                    selfUploadListener.onUploadStart(beanInfo);
            }

            @Override
            public void onLoadProgress(BaseBeanInfo beanInfo) {
                if (selfUploadListener != null && !callCancel)
                    selfUploadListener.onUploading(beanInfo);
            }

            @Override
            public String onLoadSuccess(BaseBeanInfo beanInfo, String jsonResult) {
                String url = null;
                currentProgressNum--;
                if (selfUploadListener != null && !callCancel) {
                    url = selfUploadListener.onUploadSuccess(beanInfo, jsonResult);
                }
                startOrNext();
                return url;
            }

            @Override
            public void onLoadError(BaseBeanInfo beanInfo, String resultData) {
                currentProgressNum--;
                if (selfUploadListener != null && !callCancel)
                    selfUploadListener.onUploadError(beanInfo);
                startOrNext();
            }

            @Override
            public void onLoadPaused(BaseBeanInfo beanInfo) {
                currentProgressNum--;
                if (selfUploadListener != null && !callCancel)
                    selfUploadListener.onUploadPaused(beanInfo);
                startOrNext();

            }
        };
    }

    /**
     * 释放
     */
    private void release() {
        currentProgressNum = 0;
        if (uploadTasks != null) {
            for (UploadTask task : uploadTasks) {
                if (task != null) {
                    task.cancel();
                }
            }
            uploadTasks.clear();
        }
    }

    /**
     * 取消上传
     */
    public void onDestroy() {
        callCancel = true;
        release();
    }
}
