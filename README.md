# DownLoader  (文件上传/下载器)
### 1.下载使用:DownloadTool
```java
downloadTool = new DownloadTool();
downloadTool.setContext(this);
downloadTool.setMaxNum(3);
downloadTool.setDownloadListener(downloadListener);
downloadTool.addDownload(BaseBeanInfo)
downloadTool.start()
```
### 2.上传使用:UploadTool(同下载)
