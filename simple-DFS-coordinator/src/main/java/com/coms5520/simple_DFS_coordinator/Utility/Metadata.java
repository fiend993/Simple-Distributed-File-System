package com.coms5520.simple_DFS_coordinator.Utility;

import jakarta.persistence.*;

@Entity
@Table
public class Metadata {
    @Id
    @SequenceGenerator(
            name = "meta_sequence",
            sequenceName = "meta_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "meta_sequence"
    )
    private long fileId;
    private String fileName;
    private long fileSize;
    private String chunkLocations;

    public Metadata() {

    }

    public Metadata(String fileName, long fileSize, String chunkLocations) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.chunkLocations = chunkLocations;
    }

    public long getFileId() {
        return fileId;
    }

    public void setFileId(long fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getChunkLocations() {
        return chunkLocations;
    }

    public void setChunkLocations(String chunkLocations) {
        this.chunkLocations = chunkLocations;
    }


}