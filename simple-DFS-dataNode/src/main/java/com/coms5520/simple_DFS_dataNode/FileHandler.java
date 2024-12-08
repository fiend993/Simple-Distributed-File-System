package com.coms5520.simple_DFS_dataNode;

import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.*;

public class FileHandler extends Thread {
    private final Socket socket;
    private final DataNode dataNode;

    public FileHandler(Socket socket, DataNode dataNode) {
        this.socket = socket;
        this.dataNode = dataNode;
    }

    @Override
    public void run() {
        try (
                InputStream is = socket.getInputStream();
                OutputStream os = socket.getOutputStream();
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
                DataInputStream dis = new DataInputStream(is);
        ) {
            String input = dis.readUTF();
            System.out.println(input);
            switch (input) {
                case "new" -> {
                    String fileName = dis.readUTF();
                    writeFile(fileName, dis);
                    bw.write("continue\n");
                    bw.flush();
                    String checksum = dis.readUTF();
                    if(checksum.equals(dataNode.getFileCheckSum().get(fileName))) {
                        bw.write("success\n");
                        bw.flush();
                    }else{
                        bw.write("error\n");
                        bw.flush();
                        deleteFile(fileName);
                    }
                }
                case "delete" -> {
                    String fileName = dis.readUTF();
                    if(deleteFile(fileName)){
                        bw.write("success\n");
                        bw.flush();
                    }else{
                        bw.write("error\n");
                        bw.flush();
                    }
                }
                case "replica" -> {
                    String fileName = dis.readUTF();
                    int port = dis.readInt();
                    if(replicaFile(fileName, port)){
                        bw.write("success\n");
                        bw.flush();
                    }else{
                        bw.write("error\n");
                        bw.flush();
                    }
                }
                case "validation" ->{
                    String fileName = dis.readUTF();
                    String checksum = dis.readUTF();
                    if(dataNode.getFileCheckSum().get(fileName).equals(checksum)){
                        bw.write("success\n");
                        bw.flush();
                    }else{
                        bw.write("error\n");
                        bw.flush();
                        deleteFile(fileName);
                    }
                }
                case "download" ->{
                    String fileName = dis.readUTF();
                    downloadFile(fileName);
                }
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } finally {
            try{
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void downloadFile(String fileName) throws IOException {
        File dir = new File(new File(".").getAbsolutePath(),"Files");
        File file = new File(dir, fileName);
        try(
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                FileInputStream fis = new FileInputStream(file)
        ){
            dos.writeLong(file.length());
            dos.flush();
            byte[] buffer = new byte[1048576];
            int bytesRead;
            while((bytesRead = fis.read(buffer)) != -1){
                dos.write(buffer, 0, bytesRead);
            }
            dos.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private boolean replicaFile(String fileName, int port) throws IOException {
        File dir = new File(new File(".").getAbsolutePath(),"Files");
        File file = new File(dir, fileName);
        try(
                Socket socket = new Socket("localhost",port);
                OutputStream output = socket.getOutputStream();
                DataOutputStream dos = new DataOutputStream(output);
                FileInputStream fis = new FileInputStream(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ){

            dos.writeUTF("new");
            dos.flush();

            dos.writeUTF(fileName + "[r]");
            dos.flush();

            dos.writeLong(file.length());
            dos.flush();

            byte[] buffer = new byte[1048576];
            int bytesRead;
            while((bytesRead = fis.read(buffer)) != -1){
                dos.write(buffer, 0, bytesRead);
            }
            dos.flush();
            String line = reader.readLine();
            dos.writeUTF(dataNode.getFileCheckSum().get(fileName));
            dos.flush();
            line = reader.readLine();
            return line.equals("success");
        }
    }

    private boolean deleteFile(String fileName) {
        File dir = new File(new File(".").getAbsolutePath(),"Files");
        File file = new File(dir, fileName);
        return file.delete();
    }
    private void writeFile(String fileName, DataInputStream dis) throws IOException, NoSuchAlgorithmException {
        File dir = new File(new File(".").getAbsolutePath(),"Files");
        File file = new File(dir, fileName);
        long fileSize = dis.readLong();
        long totalBytesRead = 0;
        byte[] buffer = new byte[1048576];
        int bytesRead;
        try(FileOutputStream fos = new FileOutputStream(file)){
            while (totalBytesRead < fileSize && (bytesRead = dis.read(buffer)) > 0) {
                fos.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }
        }
        dataNode.getFileCheckSum().put(fileName, dataNode.computeChecksum(file));
    }
}
