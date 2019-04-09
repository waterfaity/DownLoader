package com.waterfairy.downloader.down;

import android.os.AsyncTask;

import com.waterfairy.downloader.base.BaseBeanInfo;
import com.waterfairy.downloader.base.ProgressBean;
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

        DownloadService downloadService = RetrofitRequest.getInstance().getDownloadRetrofit();
        String rangeHeader = "bytes=" + beanInfo.getCurrentLength() + "-";


        try {
            call = downloadService.download(rangeHeader, beanInfo.getUrl());
            Response<ResponseBody> execute = call.execute();

            new FileWriter().writeFile(new FileWriter.OnFileWriteListener() {
                @Override
                public void onFileWriteSuccess() {
                    progressBean.setState(ProgressBean.STATE_RESULT).setResultCode()
                    progressBean = new ProgressBean(ProgressBean.STATE_RESULT, beanInfo).setResultCode(execute.code()).setResultData(response.body().string());

                }

                @Override
                public void onFileWriteError(String errMsg) {
                    progressBean = new ProgressBean(ProgressBean.STATE_RESULT, beanInfo).setResultCode(execute.code()).setResultData(errMsg);

                }
            }, execute.body(), beanInfo, beanInfo.getCurrentLength(), beanInfo.getTotalLength());


        } catch (IOException e) {
            e.printStackTrace();
            //下载异常
            if (isPaused) {
                progressBean = new ProgressBean(ProgressBean.STATE_PAUSED, beanInfo).setResultCode(BaseBeanInfo.ERROR_CODE).setResultData("暂停下载");
            } else {
                progressBean = new ProgressBean(ProgressBean.STATE_RESULT, beanInfo).setResultCode(BaseBeanInfo.ERROR_CODE).setResultData("未知异常");
            }
        }
        return progressBean;

    }

    @Override
    protected void onPostExecute(ProgressBean progressBean) {
        super.onPostExecute(progressBean);

        if (onUploadListener != null) {
            //下载结束
            if (progressBean.getResultCode() != 200) {
                //下载失败  1:暂停;2:失败
                if (progressBean.getState() == ProgressBean.STATE_PAUSED) {
                    progressBean.getBeanInfo().setState(BaseBeanInfo.STATE_PAUSED);
                    onUploadListener.onLoadPaused(beanInfo);
                } else {
                    progressBean.getBeanInfo().setState(BaseBeanInfo.STATE_LOADING);
                    onUploadListener.onLoadError(beanInfo, progressBean.getResultData());
                }
            } else {
                //下载成功
                progressBean.getBeanInfo().setUrlIfNotNull(onUploadListener.onLoadSuccess(beanInfo, progressBean.getResultData()));
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
         * 需要解析后返回url
         *
         * @param beanInfo
         * @param jsonResult
         * @return
         */
        String onLoadSuccess(BaseBeanInfo beanInfo, String jsonResult);

        void onLoadError(BaseBeanInfo beanInfo, String resultData);

        void onLoadPaused(BaseBeanInfo beanInfo);
    }

}
