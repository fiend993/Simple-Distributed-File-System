package com.coms5520.simple_DFS_coordinator.Service;

import com.coms5520.simple_DFS_coordinator.Interface.MetadataRepository;
import com.coms5520.simple_DFS_coordinator.Utility.DataNode;
import com.coms5520.simple_DFS_coordinator.Utility.Metadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Meta;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.net.*;

import org.json.JSONObject;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetadataService {

    private final MetadataRepository repository;

    @Autowired
    public MetadataService(MetadataRepository repository) {
        this.repository = repository;
    }

    public int preUpload(JSONObject preData, int numOfNode) {
        Optional<Metadata> fileOptional = repository.findFileByFileName(preData.getString("name"));
        if (fileOptional.isPresent()) {
            return 0;
        }else{
            //long fileSize = preData.getLong("size");
            int chunkNum = 3;
            if(numOfNode < 6){
                throw new IllegalArgumentException("Not enough data nodes online");
            }
            return chunkNum;
        }
    }

    public boolean isFileExist(String fileName) {
        Optional<Metadata> fileOptional = repository.findFileByFileName(fileName);
        return fileOptional.isPresent();
    }

    public Metadata saveMetadata(JSONObject data) {
        String name = data.getString("name");
        Long size = data.getLong("size");
        String chunkCheckSum = data.getString("chunkCheckSum");
        int numOfChunk = data.getInt("chunkNumber");
        StringBuilder builder = new StringBuilder();
        for(int i = 1; i <= 6; i++){
            builder.append("node");
            builder.append(i);
            builder.append(",");
        }
        Metadata metadata = new Metadata(name,size,builder.toString());
        return repository.save(metadata);
    }

    public Optional<Metadata> getMetadata(Long fileId) {
        return repository.findById(fileId);
    }

    @Transactional
    public void deleteMetadata(String fileName, ConcurrentHashMap<String, DataNode> nodes) {
        for(int i = 1; i < 4; i++){
            try (
                    Socket socket = new Socket("localhost", nodes.get("node" + i).getPort());
                    InputStream input = socket.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                    OutputStream output = socket.getOutputStream();
                    DataOutputStream dos = new DataOutputStream(output);
                    Socket socket2 = new Socket("localhost", nodes.get("node" + (i+3)).getPort());
                    InputStream input2 = socket2.getInputStream();
                    BufferedReader reader2 = new BufferedReader(new InputStreamReader(input2));
                    OutputStream output2 = socket2.getOutputStream();
                    DataOutputStream dos2 = new DataOutputStream(output2);
            ) {
                dos.writeUTF("delete");
                dos.flush();
                dos.writeUTF(fileName + ".part" + (i-1));
                dos.flush();
                System.out.println("node" + i + ": " + reader.readLine());
                dos2.writeUTF("delete");
                dos2.flush();
                dos2.writeUTF(fileName + ".part" + (i-1) + "[r]");
                dos2.flush();
                System.out.println("node" + (i+3) + ": " + reader2.readLine());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        repository.deleteFileByFileName(fileName);
    }

    public List<Metadata> getAllMetadata() {
        return repository.findAll();
    }

    public void startReplica(int fileId, ConcurrentHashMap<String, DataNode> nodes) throws IOException {
        Optional<Metadata> meta = repository.findById((long)fileId);
        if (meta.isPresent()) {
            Metadata metadata = meta.get();
            for(int i = 1; i < 4; i++){
                try (
                        Socket socket = new Socket("localhost", nodes.get("node" + i).getPort());
                        InputStream input = socket.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                        OutputStream output = socket.getOutputStream();
                        DataOutputStream dos = new DataOutputStream(output);
                ) {
                    dos.writeUTF("replica");
                    dos.flush();
                    dos.writeUTF(metadata.getFileName() + ".part" + (i-1));
                    dos.flush();
                    dos.writeInt(nodes.get("node"+(i+3)).getPort());
                    dos.flush();
                    String response = reader.readLine();
                    System.out.println(response);
                }
            }
        }
    }
}
