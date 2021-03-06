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
    private OnUploadInt selfUploadListener;//监听
    private boolean callCancel;//是否调用取消
    private Context context;
    private OnUploadInt onUploadListener;

    public void setContext(Context context) {
        this.context = context;
    }


    private int maxNum = 1;
    private int currentProgressNum;

    public UploadTool setMaxNum(int maxNum) {
        if (maxNum > 5) maxNum = 5;
        this.maxNum = maxNum;
        return this;
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
        if (TextUtils.isEmpty(beanInfo.getUrl()) && !TextUtils.isEmpty(beanInfo.getFilePath()) && new File(beanInfo.getFilePath()).exists()) {
            //可以下载
            //判断任务中是否已经存在
            UploadTask uploadTask = checkExist(beanInfo.getFilePath());
            if (uploadTask == null) {
                uploadTasks.add(new UploadTask(beanInfo).setOnUploadListener(getUploadOneListener()));
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
     * 启动
     */
    public void start() {
        startOrNext();
    }


    /**
     * 已经存在的任务或执行过的任务 且状体为暂停/错误的请求
     *
     * @param filePath
     */
    public void restart(String filePath) {
        restart(false, filePath);
    }

    /**
     * 已经存在的任务或执行过的任务 且状体为暂停/错误的请求
     */
    public void restartAll() {
        restart(true, null);
    }

    public void restart(boolean all, String filePath) {
        boolean needReStart = false;
        if (uploadTasks != null && uploadTasks.size() > 0) {
            for (int i = 0; i < uploadTasks.size(); i++) {
                UploadTask uploadTask = uploadTasks.get(i);
                if (all || TextUtils.equals(uploadTask.getBeanInfo().getFilePath(), filePath)) {
                    if (uploadTask.getBeanInfo().getState() == BaseBeanInfo.STATE_PAUSED || uploadTask.getBeanInfo().getState() == BaseBeanInfo.STATE_ERROR) {
                        uploadTask.getBeanInfo().setState(BaseBeanInfo.STATE_WAITING);
                        uploadTasks.remove(i);
                        uploadTask = new UploadTask(uploadTask.getBeanInfo()).setOnUploadListener(getUploadOneListener());
                        uploadTasks.add(i, uploadTask);
                        sendIntent(uploadTask.getBeanInfo(), BaseBeanInfo.STATE_WAITING);
                        needReStart = true;
                    }
                    if (!all) {
                        break;
                    }
                }
            }
            if (needReStart)
                startOrNext();
        }
    }

    /**
     * 开始下一个下载
     */
    private synchronized void startOrNext() {

        if (currentProgressNum >= maxNum) return;
        int[] sizes = getSizes();
        if (sizes[SIZE_TOTAL] == 0) return;
        if (sizes[SIZE_TOTAL] > sizes[SIZE_SUCCESS] + sizes[SIZE_ERROR] + sizes[SIZE_PAUSED]) {
            //任务未执行完
            for (int i = 0; i < uploadTasks.size(); i++) {
                UploadTask task = uploadTasks.get(i);
                //排除以下条件
                if (task != null//任务不为空
                        && !task.isExecuted()//任务未执行
                        && TextUtils.isEmpty(task.getBeanInfo().getUrl())//有上传链接
                        && task.getBeanInfo().getState() != BaseBeanInfo.STATE_ERROR//下载失败
                        && task.getBeanInfo().getState() != BaseBeanInfo.STATE_PAUSED//下载暂停
                        && task.getBeanInfo().getState() != BaseBeanInfo.STATE_SUCCESS) {//下载成功
                    currentProgressNum++;
                    if (selfUploadListener != null && !callCancel)
                        selfUploadListener.onUploadStart(task.getBeanInfo());
                    task.execute();
                    if (currentProgressNum >= maxNum) break;
                }
            }
        } else {
            //任务已经执行完毕
            if (onUploadListener != null)
                onUploadListener.onUploadAll();
        }
    }


    public void pause(String path) {
        executePause(false, path);
    }

    /**
     * 暂停所有任务
     */
    public void pauseAll() {
        executePause(true, null);
    }

    /**
     * 执行暂停
     *
     * @param pauseAll
     * @param filePath
     */
    private void executePause(boolean pauseAll, String filePath) {
        if (uploadTasks != null) {
            for (int i = 0; i < uploadTasks.size(); i++) {
                UploadTask uploadTask = uploadTasks.get(i);
                BaseBeanInfo beanInfo = uploadTask.getBeanInfo();
                //全部暂停 或 符合条件的任务  执行暂停
                boolean pause = pauseAll || (TextUtils.equals(uploadTask.getBeanInfo().getFilePath(), filePath));
                //可以暂停的状态  STATE_WAITING / STATE_START / STATE_LOADING 其他状态不需要暂停  且不需要发送广播
                if (pause && (beanInfo.getState() == BaseBeanInfo.STATE_WAITING || beanInfo.getState() == BaseBeanInfo.STATE_START || beanInfo.getState() == BaseBeanInfo.STATE_LOADING)) {
                    boolean executePause = uploadTask.pause();
                    if (!executePause) {
                        //如果没有执行暂停 发送暂停广播(执行暂停的会在asyncTask中回调onLoadPaused())
                        sendIntent(uploadTask.getBeanInfo(), BaseBeanInfo.STATE_PAUSED);
                    }
                    //如果非全部执行暂停 退出
                    if (!pauseAll) break;
                }
            }
        }

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
                if (TextUtils.equals(task.getBeanInfo().getFilePath(), path))
                    return task;
            }
        }
        return null;
    }


    /**
     * 获取上传任务
     *
     * @param filePath
     * @return
     */
    public UploadTask getUploadTask(String filePath) {
        if (uploadTasks != null && uploadTasks.size() > 0) {
            for (int i = 0; i < uploadTasks.size(); i++) {
                UploadTask uploadTask = uploadTasks.get(i);
                if (TextUtils.equals(uploadTask.getBeanInfo().getFilePath(), filePath))
                    return uploadTask;
            }
        }
        return null;
    }

    public List<UploadTask> getUploadTasks() {
        return uploadTasks;
    }

    /**
     * 获取对应信息
     *
     * @param filePath
     * @return
     */
    public BaseBeanInfo getBeanInfo(String filePath) {
        UploadTask uploadTask = getUploadTask(filePath);
        if (uploadTask != null) return uploadTask.getBeanInfo();
        return null;
    }


    public static final int SIZE_TOTAL = 0;
    public static final int SIZE_WAITING = 1;
    public static final int SIZE_LOADING = 2;
    public static final int SIZE_PAUSED = 3;
    public static final int SIZE_SUCCESS = 4;
    public static final int SIZE_ERROR = 5;

    /**
     * 获取对应的数量
     * 对外开放
     * 执行完的状态
     * total = paused + success + error
     *
     * @return
     */
    public int[] getSizes() {
        int totalSize = 0;
        int waiting = 0;
        int loading = 0;
        int pausedSize = 0;
        int successSize = 0;
        int errorSize = 0;
        if (uploadTasks != null) {
            totalSize = uploadTasks.size();
            for (int i = 0; i < uploadTasks.size(); i++) {
                switch (uploadTasks.get(i).getBeanInfo().getState()) {
                    case BaseBeanInfo.STATE_WAITING:
                        waiting++;
                        break;
                    case BaseBeanInfo.STATE_START:
                    case BaseBeanInfo.STATE_LOADING:
                        loading++;
                        break;
                    case BaseBeanInfo.STATE_PAUSED:
                        pausedSize++;
                        break;
                    case BaseBeanInfo.STATE_SUCCESS:
                        successSize++;
                        break;
                    case BaseBeanInfo.STATE_ERROR:
                        errorSize++;
                        break;
                }
            }
        }
        return new int[]{totalSize, waiting, loading, pausedSize, successSize, errorSize};
    }


    /**
     * 内部调用
     *
     * @return
     */
    private UploadTask.OnUploadListener getUploadOneListener() {
        return new UploadTask.OnUploadListener() {
            @Override
            public void onUploadStart(BaseBeanInfo beanInfo) {
                if (selfUploadListener != null && !callCancel)
                    selfUploadListener.onUploadStart(beanInfo);
            }

            @Override
            public void onUploadProgress(BaseBeanInfo beanInfo) {
                if (selfUploadListener != null && !callCancel)
                    selfUploadListener.onUploading(beanInfo);
            }

            @Override
            public void onUploadSuccess(BaseBeanInfo beanInfo, String jsonResult) {
                currentProgressNum--;
                if (selfUploadListener != null && !callCancel) {
                    selfUploadListener.onUploadSuccess(beanInfo, jsonResult);
                }
                startOrNext();
            }

            @Override
            public void onUploadError(BaseBeanInfo beanInfo, String errMsg) {
                currentProgressNum--;
                if (selfUploadListener != null && !callCancel)
                    selfUploadListener.onUploadError(beanInfo);
                startOrNext();
            }

            @Override
            public void onUploadPaused(BaseBeanInfo beanInfo) {
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
        if (uploadTasks != null) {
            for (UploadTask task : uploadTasks) {
                if (task != null) {
                    task.pause();
                }
            }
            uploadTasks.clear();
        }
        currentProgressNum = 0;
    }

    /**
     * 取消上传
     */
    public void onDestroy() {
        callCancel = true;
        release();
    }


    /**
     * 发送广播
     *
     * @param mediaResBean
     * @param state
     * @param progress
     */
    private void sendIntent(BaseBeanInfo mediaResBean, int state, int progress) {
        if (mediaResBean != null) mediaResBean.setState(state);
        if (context == null || mediaResBean == null || TextUtils.isEmpty(mediaResBean.getFilePath()))
            return;
        Intent intent = new Intent();
        intent.setAction(mediaResBean.getFilePath());
        if (state == BaseBeanInfo.STATE_LOADING) {
            intent.putExtra(STR_STATE_PROGRESS, progress);
        }
        intent.putExtra(STR_STATE_TAG, state);
        context.sendBroadcast(intent);
    }

    /**
     * 发送广播
     *
     * @param mediaResBean
     * @param state
     */
    private void sendIntent(BaseBeanInfo mediaResBean, int state) {
        sendIntent(mediaResBean, state, 0);
    }

    /**
     * 设置监听
     *
     * @param listener
     */
    public void setUploadListener(OnUploadInt listener) {
        this.onUploadListener = listener;
        this.selfUploadListener = new OnUploadInt() {
            @Override
            public void onUploadStart(BaseBeanInfo mediaResBean) {
                sendIntent(mediaResBean, BaseBeanInfo.STATE_START);
                if (onUploadListener != null) {
                    onUploadListener.onUploadStart(mediaResBean);
                }
            }

            @Override
            public void onUploadSuccess(BaseBeanInfo mediaResBean, String jsonResult) {
                sendIntent(mediaResBean, BaseBeanInfo.STATE_SUCCESS);
                if (onUploadListener != null) {
                    onUploadListener.onUploadSuccess(mediaResBean, jsonResult);
                }
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


    public static abstract class OnUploadListener implements OnUploadInt {


        @Override
        public void onUploadStart(BaseBeanInfo mediaResBean) {

        }

        @Override
        public abstract void onUploading(BaseBeanInfo mediaResBean);

        @Override
        public void onUploadPaused(BaseBeanInfo beanInfo) {

        }

        @Override
        public abstract void onUploadSuccess(BaseBeanInfo mediaResBean, String jsonResult);

        @Override
        public abstract void onUploadError(BaseBeanInfo mediaResBean);

        @Override
        public void onUploadAll() {

        }
    }


    /**
     * 监听
     */
    public interface OnUploadInt {

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
        void onUploadSuccess(BaseBeanInfo mediaResBean, String jsonResult);

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
}
