package com.waterfairy.downloader.down;

import com.waterfairy.downloader.base.BaseBeanInfo;
import com.waterfairy.downloader.base.ProgressListener;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

/**
 * Created by shui on 2017/4/26.
 */

public class DownloadResponseBody extends ResponseBody {

    private ResponseBody responseBody;
    private ProgressListener progressListener;
    private BufferedSource bufferedSource;
    private int responseCode;
    private BaseBeanInfo beanInfo;
    private long totalLen;

    public DownloadResponseBody(ResponseBody responseBody, BaseBeanInfo beanInfo,int responseCode, ProgressListener progressListener) {
        this.responseBody = responseBody;
        this.beanInfo = beanInfo;
        this.progressListener = progressListener;
        this.responseCode = responseCode;
    }

    @Override
    public MediaType contentType() {
        return responseBody.contentType();
    }

    @Override
    public long contentLength() {
        if (totalLen == 0)
            totalLen = responseBody.contentLength();
        return totalLen;
    }

    @Override
    public BufferedSource source() {
        if (bufferedSource == null) bufferedSource = Okio.buffer(source(responseBody.source()));
        return bufferedSource;
    }

    private Source source(BufferedSource source) {
        return new ForwardingSource(source) {
            long readTotal = 0l;

            @Override
            public long read(Buffer sink, long byteCount) throws IOException {
                if (responseCode != 404) {
                    long bytesRead = super.read(sink, byteCount);
                    readTotal += bytesRead != -1 ? bytesRead : 0;
                    if (null != progressListener){
                        beanInfo.setCurrentLength(readTotal);
                        beanInfo.setTotalLength(contentLength());
                        progressListener.onProgressing(beanInfo, contentLength(), readTotal);
                    }
                    return bytesRead;
                } else {
                    return -1;
                }
            }
        };
    }
}
