package com.waterfairy.downloader.upload;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.QueryMap;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

/**
 * Created by water_fairy on 2017/6/26.
 * 995637517@qq.com
 */

public interface UploadService {
    @Streaming
    @POST()
    @Multipart
    Call<ResponseBody> upload(@Url String url,
                              @Part MultipartBody.Part part);

    /**
     * 断点上传
     *
     * @param url
     * @param start "bytes=" + currentLength + "-"
     * @param part
     * @return
     */
    @Streaming
    @POST()
    @Multipart
    Call<ResponseBody> upload(@Url String url,
                              @Header("range") String start,
                              @Part MultipartBody.Part part);

    @Streaming
    @POST()
    @Multipart
    Call<ResponseBody> upload(@Url String url,
                              @Header("RANGE") String start,
                              @QueryMap Map<String, String> options,
                              @Part MultipartBody.Part part);

    @Streaming
    @POST()
    @Multipart
    Call<ResponseBody> uploadNoParam(@Url String url,
                                     @Header("RANGE") String start,
                                     @QueryMap Map<String, String> options,
                                     @PartMap Map<String, RequestBody> externalFileParameters);

    @POST()
    @Multipart
    Call<ResponseBody> uploadMul(@Url String url,
                                 @Header("RANGE") String start,
                                 @QueryMap Map<String, String> options,
                                 @Part Map<String, RequestBody> externalFileParameters);
}
