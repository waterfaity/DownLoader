//package com.waterfairy.downloader.down;
//
//import com.waterfairy.downloader.base.BaseBeanInfo;
//import com.waterfairy.downloader.base.ProgressBean;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.io.RandomAccessFile;
//import java.nio.MappedByteBuffer;
//import java.nio.channels.FileChannel;
//
//import okhttp3.ResponseBody;
//
///**
// * Created by shui on 2017/5/6.
// */
//
//public class FileWriter3 {
//
//    /**
//     * 文件存储
//     *
//     * @param responseBody
//     * @param info
//     * @param progressBean
//     */
//    public void writeFile(ResponseBody responseBody, BaseBeanInfo info, long currentLength, long totalLength, ProgressBean progressBean) {
//        String errMsg = null;
//        boolean success = true;
//        if (responseBody != null) {
//            //创建文件
//            File file = new File(info.getPath());
//            if (success = canSave(file)) {
//                FileChannel channelOut = null;
//                if (totalLength == 0) totalLength = responseBody.contentLength();
//                if (totalLength != 0) {
//                    MappedByteBuffer mappedByteBuffer = null;
//                    RandomAccessFile randomAccessFile = null;
//                    try {
//                        randomAccessFile = new RandomAccessFile(file, "rw");
//                        channelOut = randomAccessFile.getChannel();
//                        mappedByteBuffer = channelOut.map(FileChannel.MapMode.READ_WRITE, currentLength, totalLength - currentLength);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                        errMsg = "本地文件文件错误";
//                        success = false;
//                    }
//
//                    if (success) {
//                        //写入文件
//                        byte[] buffer = new byte[1024 * 512];
//                        int len;
//                        int record = 0;
//
//                        //网络读取流
//                        try {
//                            while ((len = responseBody.byteStream().read(buffer)) != -1) {
//                                mappedByteBuffer.put(buffer, 0, len);
//                                record += len;
//                            }
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                            errMsg = "文件流读取失败";
//                            success = false;
//                        }
//                        if (success) {
//                            //写入文件
//                            try {
//                                responseBody.byteStream().close();
//                            } catch (IOException e) {
//                                success = false;
//                                e.printStackTrace();
//                                errMsg = "文件流写入失败";
//                            }
//                        }
//                        //刷新流
//                        if (success) {
//                            try {
//                                channelOut.close();
//                                randomAccessFile.close();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                                success = false;
//                            }
//                        }
//                    } else {
//                        errMsg = "文件大小获取为0";
//                    }
//                }
//            } else {
//                errMsg = "文件创建失败";
//            }
//        } else {
//            errMsg = "responseBody == null";
//        }
//
//        if (success) {
//            progressBean.setState(ProgressBean.STATE_RESULT);
//            fileWriteListener.onFileWriteSuccess();
//        } else {
//            fileWriteListener.onFileWriteError(errMsg);
//        }
//
//    }
//
//    public static class ResultBean {
//        private String msg;
//        private boolean success;
//    }
//
//    /**
//     * 创建文件
//     *
//     * @param file
//     * @return
//     */
//    private boolean canSave(File file) {
//        boolean canSave = false;
//        if (file.exists()) {
//            canSave = true;
//        } else {
//            if (!file.getParentFile().exists()) {
//                canSave = file.getParentFile().mkdirs();
//            } else {
//                canSave = true;
//            }
//            if (canSave) {
//                try {
//                    canSave = file.createNewFile();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    canSave = false;
//                }
//            }
//        }
//        return canSave;
//    }
//
//
//}
