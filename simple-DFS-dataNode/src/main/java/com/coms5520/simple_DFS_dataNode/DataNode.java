package com.coms5520.simple_DFS_dataNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.*;

public class DataNode {
    private final ConcurrentHashMap<String,String> fileCheckSum = new ConcurrentHashMap<>();

    public DataNode(){
    }

    private void star() throws IOException, NoSuchAlgorithmException {
        File dir = new File(new File(".").getAbsolutePath(),"Files");

        if(dir.exists() && dir.isDirectory()){
            File[] files = dir.listFiles();
            if(files != null){
                for(File file : files){
                    String fileName = file.getName();
                    String checkSum = computeChecksum(file);
                    fileCheckSum.put(fileName, checkSum);
                }
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(10);

        try(ServerSocket serverSocket = new ServerSocket(0)){
            HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:8080"+"/api/dataNodes/" + serverSocket.getLocalPort()).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            int responseCode = conn.getResponseCode();
            if(responseCode == 200){
                System.out.println("Successfully connected to the coordinator");
            }else{
                System.out.println("Failed to connect to the coordinator");
                return;
            }
            while(true){
                Socket socket = serverSocket.accept();
                executor.execute(new FileHandler(socket,this));
            }
        }finally {
            executor.shutdown();
        }
    }

    public ConcurrentHashMap<String,String> getFileCheckSum(){
        return fileCheckSum;
    }

    public String computeChecksum(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) > 0) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        byte[] digestBytes = digest.digest();
        StringBuilder checksum = new StringBuilder();
        for (byte b : digestBytes) {
            checksum.append(String.format("%02X", b));
        }
        return checksum.toString();
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        DataNode dataNode = new DataNode();
        dataNode.star();
    }
}
