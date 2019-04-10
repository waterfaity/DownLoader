package com.waterfairy.downloader.base;


import com.waterfairy.downloader.down.DownloadInterceptor;
import com.waterfairy.downloader.down.DownloadService;
import com.waterfairy.downloader.upload.UploadService;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

/**
 * Created by water_fairy on 2017/5/18.
 * 995637517@qq.com
 */

public class RetrofitRequest {
    private static RetrofitRequest retrofitRequest;
    private UploadService uploadRetrofitService;
    private String baseUrl;

    private RetrofitRequest() {

    }

    public static RetrofitRequest getInstance() {
        if (retrofitRequest == null) retrofitRequest = new RetrofitRequest();
        return retrofitRequest;
    }

    public void initBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public UploadService getUploadRetrofit() {
        if (uploadRetrofitService == null) {
            uploadRetrofitService = buildClient(null).create(UploadService.class);
        }
        return uploadRetrofitService;
    }

    public DownloadService getDownloadRetrofit(BaseBeanInfo beanInfo, ProgressListener progressListener) {
        return buildClient(new DownloadInterceptor(beanInfo, progressListener)).create(DownloadService.class);
    }

    private Retrofit buildClient(DownloadInterceptor downloadInterceptor) {
        OkHttpClient.Builder okHttpClient = new OkHttpClient().newBuilder();
        okHttpClient.connectTimeout(15000, TimeUnit.MILLISECONDS);
        okHttpClient.readTimeout(15000, TimeUnit.MILLISECONDS);
        if (downloadInterceptor != null)
            okHttpClient.addInterceptor(downloadInterceptor);
        return new Retrofit.Builder().baseUrl(baseUrl).client(okHttpClient.build()).build();
    }
}
