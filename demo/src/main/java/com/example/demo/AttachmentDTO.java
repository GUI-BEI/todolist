package com.example.demo;

public class AttachmentDTO {
    private Long id;
    private Long taskId;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String uploadTime;
    private String downloadUrl;

    public AttachmentDTO() {}

    public AttachmentDTO(TaskAttachment attachment) {
        this.id = attachment.getId();
        this.taskId = attachment.getTaskId();
        this.fileName = attachment.getFileName();
        this.fileSize = attachment.getFileSize();
        this.fileType = attachment.getFileType();
        this.uploadTime = attachment.getUploadTime().toString();
        this.downloadUrl = "/attachments/" + attachment.getFilePath();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getUploadTime() { return uploadTime; }
    public void setUploadTime(String uploadTime) { this.uploadTime = uploadTime; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
}