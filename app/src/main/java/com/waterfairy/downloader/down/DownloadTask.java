package com.waterfairy.downloader.down;

import android.os.AsyncTask;

import com.waterfairy.downloader.base.BaseBeanInfo;
import com.waterfairy.downloader.base.ProgressBean;
import com.waterfairy.downloader.base.ProgressListener;
import com.waterfairy.downloader.base.RetrofitRequest;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * @author water_fairy
 * @email 995637517@qq.com
 * @date 2019/4/4 16:11
 * @info: 上传异步处理
 */
public class DownloadTask extends AsyncTask<BaseBeanInfo, ProgressBean, ProgressBean> {
    private Call<ResponseBody> call;
    private BaseBeanInfo beanInfo;
    private OnDownloadListener onUploadListener;
    private boolean isExecuted;//是否已经执行
    private boolean isPaused;//暂停状态

    public DownloadTask setOnUploadListener(OnDownloadListener onUploadListener) {
        this.onUploadListener = onUploadListener;
        return this;
    }

    public DownloadTask(BaseBeanInfo beanInfo) {
        this.beanInfo = beanInfo;
    }

    public BaseBeanInfo getBeanInfo() {
        return beanInfo;
    }

    @Override
    protected ProgressBean doInBackground(BaseBeanInfo... beanInfos) {
        final ProgressBean progressBean = new ProgressBean(beanInfo);

        beanInfo.setState(BaseBeanInfo.STATE_LOADING);
        DownloadService downloadService = RetrofitRequest.getInstance().getDownloadRetrofit(beanInfo, new ProgressListener() {
            @Override
            public void onProgressing(BaseBeanInfo beanInfo, long total, long current) {

                publishProgress(new ProgressBean(ProgressBean.STATE_PROGRESS, beanInfo));
            }
        });
        String rangeHeader = "bytes=" + beanInfo.getCurrentLength() + "-";
        boolean success = true;
        String msg = null;
        try {
            call = downloadService.download(rangeHeader, beanInfo.getUrl());
            Response<ResponseBody> execute = call.execute();
            int code = execute.code();
            if (code != 404) {
                FileWriter.ResultBean resultBean = new FileWriter().write(execute.body(), beanInfo.getPath(), beanInfo.getCurrentLength(), beanInfo.getTotalLength());
                if (success = resultBean.isSuccess()) {
                    //成功
                    progressBean.setState(ProgressBean.STATE_RESULT).setResultCode(code);
                } else {
                    msg = resultBean.getMsg();
                }
            } else {
                success = false;
                msg = "url 404";
            }
        } catch (IOException e) {
            success = false;
            e.printStackTrace();
            msg = "request error";
        }
        //下载异常
        if (!success) {
            if (isPaused) {
                progressBean.setState(ProgressBean.STATE_PAUSED).setResultCode(BaseBeanInfo.ERROR_CODE).setResultData("暂停下载");
            } else {
                progressBean.setState(ProgressBean.STATE_RESULT).setResultCode(BaseBeanInfo.ERROR_CODE).setResultData(msg);
            }
        }
        return progressBean;

    }

    @Override
    protected void onPostExecute(ProgressBean progressBean) {
        super.onPostExecute(progressBean);

        if (onUploadListener != null) {
            //下载结束
            if (progressBean.getResultCode() != 206) {
                //下载失败  1:暂停;2:失败
                if (progressBean.getState() == ProgressBean.STATE_PAUSED) {
                    progressBean.getBeanInfo().setState(BaseBeanInfo.STATE_PAUSED);
                    onUploadListener.onLoadPaused(beanInfo);
                } else {
                    beanInfo.setErrMsg(progressBean.getResultData());
                    progressBean.getBeanInfo().setState(BaseBeanInfo.STATE_LOADING);
                    onUploadListener.onLoadError(beanInfo);
                }
            } else {
                //下载成功
                onUploadListener.onLoadSuccess(beanInfo);
            }
        }
    }

    @Override
    protected void onProgressUpdate(ProgressBean... values) {
        super.onProgressUpdate(values);
        ProgressBean beanInfo = values[0];
        if (onUploadListener != null) {
            if (beanInfo.getState() == ProgressBean.STATE_PROGRESS) {
                //下载进度
                onUploadListener.onLoadProgress(beanInfo.getBeanInfo());
            } else if (beanInfo.getState() == ProgressBean.STATE_START) {
                //下载开始
                onUploadListener.onLoadStart(beanInfo.getBeanInfo());
            }
        }
    }


    /**
     * 并发线程
     */
    public void execute() {
        isPaused = false;
        if (isExecuted) return;
        isExecuted = true;
        //并发线程
        super.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    public boolean isExecuted() {
        return isExecuted;
    }

    /**
     * 暂停
     *
     * @param checkState
     * @return
     */
    public boolean pause(boolean checkState) {
        if (!checkState//不执行检查状体
                || beanInfo.getState() == BaseBeanInfo.STATE_WAITING//执行检查状态 等待中
                || beanInfo.getState() == BaseBeanInfo.STATE_START//执行检查状态 开始
                || beanInfo.getState() == BaseBeanInfo.STATE_LOADING) {//执行检查状态 传输中

            //已经暂停 返回
            if (isPaused) return false;
            isPaused = true;
            //还未开始 返回
            if (!isExecuted) return false;
            //已经开始 取消任务
            return cancel();
        }
        return false;
    }

    /**
     * 暂停
     */
    public boolean pause() {
        return pause(false);
    }

    /**
     * 取消 停止上传
     */
    public boolean cancel() {
        if (call != null && isExecuted) {
            call.cancel();
            return true;
        } else {
            return false;
        }
    }


    public interface OnDownloadListener {
        void onLoadStart(BaseBeanInfo beanInfo);

        void onLoadProgress(BaseBeanInfo beanInfo);

        /**
         * @param beanInfo
         * @return
         */
        void onLoadSuccess(BaseBeanInfo beanInfo);

        void onLoadError(BaseBeanInfo beanInfo);

        void onLoadPaused(BaseBeanInfo beanInfo);
    }
}
