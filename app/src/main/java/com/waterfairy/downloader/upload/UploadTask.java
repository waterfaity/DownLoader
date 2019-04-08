package com.waterfairy.downloader.upload;

import android.os.AsyncTask;

import com.waterfairy.downloader.base.BaseBeanInfo;
import com.waterfairy.downloader.base.ProgressBean;
import com.waterfairy.downloader.base.ProgressListener;
import com.waterfairy.downloader.base.RetrofitRequest;

import java.io.File;
import java.io.IOException;

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
    private boolean canAutoExecute = true;
    private boolean isExecute;//是否已经执行
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

        if (canAutoExecute) {
            beanInfo.setState(BaseBeanInfo.STATE_LOADING);
            File file = new File(beanInfo.getPath());
            RequestBody sourceBody = RequestBody.create(MediaType.parse("application/otcet-stream"), file);
            UploadRequestBody uploadRequestBody = new UploadRequestBody(sourceBody, beanInfo, new ProgressListener() {
                @Override
                public void onProgressing(BaseBeanInfo beanInfo, long total, long current) {
                    beanInfo.setCurrentLength(current);
                    beanInfo.setTotalLength(total);
                    publishProgress(new ProgressBean(ProgressBean.STATE_PROGRESS, beanInfo));
                }
            });
            MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                    "file",
                    file.getName(),
                    uploadRequestBody);
            UploadService uploadService = RetrofitRequest.getInstance().getUploadRetrofit();
            if (beanInfo.getCurrentLength() != 0) {
                call = uploadService.upload(beanInfo.getUploadUrl(), beanInfo.getCurrentLength() + "", null, filePart);
            } else {
                call = uploadService.upload(beanInfo.getUploadUrl(), filePart);
            }
            try {
                //开始下载
                publishProgress(new ProgressBean(ProgressBean.STATE_START, beanInfo));
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
        } else {
            if (isPaused) {
                progressBean = new ProgressBean(ProgressBean.STATE_PAUSED, beanInfo).setResultCode(BaseBeanInfo.ERROR_CODE).setResultData("暂停下载");
            } else {
                progressBean = new ProgressBean(ProgressBean.STATE_RESULT, beanInfo).setResultCode(BaseBeanInfo.ERROR_CODE).setResultData("未知异常");
            }
            return progressBean;
        }

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
        isExecute = false;
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


    public void setCanAutoExecute(boolean canAutoExecute) {
        this.canAutoExecute = canAutoExecute;
    }

    public void execute() {
        isPaused = false;
        if (isExecute || !canAutoExecute) return;
        isExecute = true;
        super.execute();
    }


    public boolean isExecute() {
        return isExecute;
    }

    /**
     * 暂停
     */
    public void pause() {
        if (isPaused) return;
        isPaused = true;
        cancel();
    }

    /**
     * 取消 停止上传
     */
    public void cancel() {
        setCanAutoExecute(false);
        if (call != null && isExecute) {
            call.cancel();
        }
    }

    public boolean getCanAutoExecute() {
        return canAutoExecute;
    }

    public interface OnUploadListener {
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
