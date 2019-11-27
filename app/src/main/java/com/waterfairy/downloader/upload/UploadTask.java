package com.waterfairy.downloader.upload;

import android.os.AsyncTask;

import com.waterfairy.downloader.base.BaseBeanInfo;
import com.waterfairy.downloader.base.ProgressBean;
import com.waterfairy.downloader.base.ProgressListener;
import com.waterfairy.downloader.base.RetrofitRequest;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * @author water_fairy
 * @email 995637517@qq.com
 * @date 2019/4/4 16:11
 * @info: 上传异步处理
 */
public class UploadTask extends AsyncTask<BaseBeanInfo, ProgressBean, ProgressBean> {
    private Call<ResponseBody> call;
    private BaseBeanInfo beanInfo;
    private OnUploadListener onUploadListener;
    private boolean isExecuted;//是否已经执行
    private boolean isPaused;//暂停状态

    public UploadTask setOnUploadListener(OnUploadListener onUploadListener) {
        this.onUploadListener = onUploadListener;
        return this;
    }

    public UploadTask(BaseBeanInfo beanInfo) {
        this.beanInfo = beanInfo;
    }

    public BaseBeanInfo getBeanInfo() {
        return beanInfo;
    }

    @Override
    protected ProgressBean doInBackground(BaseBeanInfo... beanInfos) {
        ProgressBean progressBean = null;

        beanInfo.setState(BaseBeanInfo.STATE_LOADING);
        File file = new File(beanInfo.getFilePath());
        RequestBody sourceBody = RequestBody.create(MediaType.parse("application/otcet-stream"), file);
        UploadRequestBody uploadRequestBody = new UploadRequestBody(sourceBody, beanInfo, new ProgressListener() {
            @Override
            public void onProgressing(BaseBeanInfo beanInfo, long total, long current) {
                publishProgress(new ProgressBean(ProgressBean.STATE_PROGRESS, beanInfo));
            }
        });
        MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                "file",
                file.getName(),
                uploadRequestBody);
        UploadService uploadService = RetrofitRequest.getInstance().getUploadRetrofit();

        HashMap<String, String> paramsHashMap = beanInfo.getParamsHashMap();

        if (beanInfo.getCurrentLength() != 0) {
            if (paramsHashMap != null) {
                call = uploadService.upload(beanInfo.getUploadUrl(), "bytes=" + beanInfo.getCurrentLength() + "-" + beanInfo.getTotalLength(), paramsHashMap, filePart);
            } else {
                call = uploadService.upload(beanInfo.getUploadUrl(), "bytes=" + beanInfo.getCurrentLength() + "-" + beanInfo.getTotalLength(), filePart);
            }
        } else {
            if (paramsHashMap != null) {
                call = uploadService.upload(beanInfo.getUploadUrl(), paramsHashMap, filePart);
            } else {
                call = uploadService.upload(beanInfo.getUploadUrl(), filePart);
            }
        }
        try {
            //启动下载
            Response<ResponseBody> response = call.execute();
            //下载结束
            progressBean = new ProgressBean(ProgressBean.STATE_RESULT, beanInfo).setResultCode(response.code()).setResultData(response.body().string());
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
                onUploadListener.onLoadSuccess(beanInfo, progressBean.getResultData());
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


    public interface OnUploadListener {
        void onLoadStart(BaseBeanInfo beanInfo);

        void onLoadProgress(BaseBeanInfo beanInfo);

        void onLoadSuccess(BaseBeanInfo beanInfo, String jsonResult);

        void onLoadError(BaseBeanInfo beanInfo, String resultData);

        void onLoadPaused(BaseBeanInfo beanInfo);
    }

}
