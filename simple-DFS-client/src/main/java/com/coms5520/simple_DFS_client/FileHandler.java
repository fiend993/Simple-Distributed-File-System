package com.coms5520.simple_DFS_client;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.json.JSONObject;

import javax.print.DocFlavor;

public class FileHandler {

    public FileHandler() {
    }

    public void uploadFile(File file, String url) throws IOException, NoSuchAlgorithmException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url+"/preUpload").openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");

        JSONObject obj = new JSONObject();
        obj.put("name", file.getName());

        try(OutputStream os = conn.getOutputStream()) {
            byte[] input = obj.toString().getBytes("UTF-8");
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = br.readLine()) != null) {
                content.append(inputLine.trim());
            }
            br.close();

            JSONObject response = new JSONObject(content.toString());
            int numberOfChunk = response.getInt("numberOfChunks");
            conn.disconnect();
            if(numberOfChunk == 0){
                System.out.println("File already exists");
                return;
            }

            ArrayList<String> chunkList = splitFile(file, numberOfChunk);
            ArrayList<String> chunkChecksumList = new ArrayList<>(chunkList.size());
            for(String chunk : chunkList) {
                chunkChecksumList.add(computeChecksum(chunk));
            }

            JSONObject metadata = new JSONObject();
            StringBuilder chunkAndNodes = new StringBuilder();
            for(int i = 0; i < numberOfChunk; i++){
                chunkAndNodes.append(chunkChecksumList.get(i));
                chunkAndNodes.append(",");
            }
            metadata.put("size", file.length());
            metadata.put("name", file.getName());
            metadata.put("chunkCheckSum", chunkAndNodes.toString());
            metadata.put("chunkNumber", numberOfChunk);

            conn = (HttpURLConnection) new URL(url+"/ready").openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            try(OutputStream os = conn.getOutputStream()) {
                byte[] input = metadata.toString().getBytes("UTF-8");
                os.write(input, 0, input.length);
            }

            responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String out;
                StringBuilder ss = new StringBuilder();
                while ((out = reader.readLine()) != null) {
                    ss.append(out.trim());
                }
                reader.close();
                conn.disconnect();

                JSONObject response2 = new JSONObject(ss.toString());
                String portNumbers = response2.getString("nodeList");
                String[] parts = portNumbers.split("[,:]");
                int chunkIndex = 0;
                for (int i = 1; i < parts.length; i += 2) {
                    try (
                            Socket socket = new Socket("localhost", Integer.parseInt(parts[i]));
                            OutputStream output = socket.getOutputStream();
                            DataOutputStream dos = new DataOutputStream(output);
                            FileInputStream fis = new FileInputStream(chunkList.get(chunkIndex));
                            BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream()))
                    ) {
                        String result = "";

                        dos.writeUTF("new");
                        dos.flush();

                        dos.writeUTF(chunkList.get(chunkIndex));
                        dos.flush();

                        File ff = new File(chunkList.get(chunkIndex));
                        dos.writeLong(ff.length());
                        dos.flush();

                        // Send file content
                        byte[] buffer = new byte[1048576];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) > 0) {
                            dos.write(buffer, 0, bytesRead);
                        }
                        dos.flush();
                        r.readLine();
                        dos.writeUTF(chunkChecksumList.get(chunkIndex));
                        dos.flush();
                        result = r.readLine();
                        if(result.equals("success")){
                            System.out.println("File uploaded successfully");
                        }else{
                            System.out.println("File not uploaded");
                        }
                        chunkIndex++;
                    }
                }

                conn = (HttpURLConnection) new URL(url+"/finished/" + response2.getInt("fileId")).openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                if(conn.getResponseCode() == HttpURLConnection.HTTP_OK){
                    conn.disconnect();
                }else{
                    conn.disconnect();
                    System.out.println("Error uploading file: " + conn.getResponseCode  ());
                }
                for(int i = 0; i < chunkList.size(); i++){
                    File f = new File(chunkList.get(i));
                    f.delete();
                }

            }else{
                System.out.println("Error uploading file: " + conn.getResponseCode  ());
            }

        } else {
            System.out.println("Error uploading file: " + conn.getResponseCode());
        }
    }

    public void retrieveFile(String fileName, String dataNodes, Boolean replica) throws IOException {
        String[] ports = dataNodes.split("[,:]");
        int chunkIndex = 0;
        for(int i = 1; i < ports.length; i+=2){
            try (
                    Socket socket = new Socket("localhost", Integer.parseInt(ports[i]));
                    OutputStream output = socket.getOutputStream();
                    InputStream input = socket.getInputStream();
                    DataInputStream dis = new DataInputStream(input);
                    DataOutputStream dos = new DataOutputStream(output);
            ) {
                dos.writeUTF("download");
                dos.flush();
                if(replica){
                    dos.writeUTF(fileName+".part" + chunkIndex + "[r]");
                    dos.flush();
                }else{
                    dos.writeUTF(fileName+".part" + chunkIndex);
                    dos.flush();
                }
                File file = new File(fileName+".part" + chunkIndex);
                long fileLength = dis.readLong();
                long totalBytesWrite = 0;
                byte[] buffer = new byte[1048576];
                int bytesRead;
                try(FileOutputStream fos = new FileOutputStream(file)){
                    while (totalBytesWrite < fileLength && (bytesRead = dis.read(buffer)) > 0) {
                        fos.write(buffer, 0, bytesRead);
                        totalBytesWrite += bytesRead;
                    }
                }
                chunkIndex++;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        File output = new File(".");
        reconstructFile(output,fileName,3);
    }

    private ArrayList<String> splitFile(File file, int chunkNumber) throws IOException {
        long fileSize = file.length();
        int chunkSizeInBytes = (int) Math.ceil((double) fileSize / chunkNumber);
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[chunkSizeInBytes];
            int bytesRead;
            int chunkCount = 0;
            ArrayList<String> chunkList = new ArrayList<>();
            while ((bytesRead = fis.read(buffer)) > 0) {
                String chunk = file.getName() + ".part" + chunkCount;
                File chunkFile = new File(file.getParent(), chunk);
                chunkList.add(chunk);
                chunkCount++;
                try (FileOutputStream fos = new FileOutputStream(chunkFile)) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            return chunkList;
        }
    }

    private String computeChecksum(String file) throws IOException, NoSuchAlgorithmException {
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

    public static void reconstructFile(File outputDir, String originalFileName, int totalChunks) throws IOException {
        File reconstructedFile = new File(outputDir, "reconstructed_" + originalFileName);
        try (FileOutputStream fos = new FileOutputStream(reconstructedFile)) {
            for (int i = 0; i < totalChunks; i++) {
                File chunkFile = new File(outputDir, originalFileName + ".part" + i);
                try (FileInputStream fis = new FileInputStream(chunkFile)) {
                    byte[] buffer = new byte[(int) chunkFile.length()];
                    int bytesRead = fis.read(buffer);
                    fos.write(buffer, 0, bytesRead);
                }
                chunkFile.delete();
            }
        }
    }
}
