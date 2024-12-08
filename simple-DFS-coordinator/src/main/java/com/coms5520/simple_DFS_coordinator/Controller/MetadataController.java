package com.coms5520.simple_DFS_coordinator.Controller;

import com.coms5520.simple_DFS_coordinator.Service.DataNodeService;
import com.coms5520.simple_DFS_coordinator.Service.MetadataService;
import com.coms5520.simple_DFS_coordinator.Utility.DataNode;
import com.coms5520.simple_DFS_coordinator.Utility.Metadata;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/metadata")
public class MetadataController {

    private MetadataService metadataService;
    private DataNodeService dataNodeService;

    @Autowired
    public MetadataController(MetadataService metadataService, DataNodeService dataNodeService) {
        this.metadataService = metadataService;
        this.dataNodeService = dataNodeService;
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Map<String, Object>> download(@PathVariable String fileName) throws IOException {
        JSONObject response = new JSONObject();
        ConcurrentHashMap<String, DataNode> nodes = dataNodeService.getDataNodes();
        if(!metadataService.isFileExist(fileName)) {
            return ResponseEntity.badRequest().build();
        }
        response.put("nodes", nodes.get("node1").getIpAddress() + ":" + nodes.get("node1").getPort()
                + "," + nodes.get("node2").getIpAddress() + ":" + nodes.get("node2").getPort() + ","
                + nodes.get("node3").getIpAddress() + ":" + nodes.get("node3").getPort() + ","
        );
        return ResponseEntity.ok(response.toMap());
    }

    @GetMapping("/downloadr/{fileName}")
    public ResponseEntity<Map<String, Object>> downloadReplica(@PathVariable String fileName) throws IOException {
        JSONObject response = new JSONObject();
        ConcurrentHashMap<String, DataNode> nodes = dataNodeService.getDataNodes();
        if(!metadataService.isFileExist(fileName)) {
            return ResponseEntity.badRequest().build();
        }
        response.put("nodes", nodes.get("node4").getIpAddress() + ":" + nodes.get("node4").getPort()
                + "," + nodes.get("node5").getIpAddress() + ":" + nodes.get("node5").getPort() + ","
                + nodes.get("node6").getIpAddress() + ":" + nodes.get("node6").getPort() + ","
        );
        return ResponseEntity.ok(response.toMap());
    }

    @PostMapping("/preUpload")
    public ResponseEntity<Map<String, Object>> preUpload(@RequestBody Map<String, Object> preData){
        JSONObject response = new JSONObject();
        response.put("numberOfChunks", metadataService.preUpload(new JSONObject(preData),dataNodeService.getNumberOfDataNodes()));
        return ResponseEntity.ok(response.toMap());
    }

    @GetMapping("/getList")
    public ResponseEntity<Map<String, Object>> getList(){
        List<Metadata> metadataList = metadataService.getAllMetadata();
        JSONObject response = new JSONObject();
        StringBuilder builder = new StringBuilder();
        for (Metadata metadata : metadataList) {
            builder.append(metadata.getFileName());
            builder.append(",");
        }
        response.put("list",builder.toString());
        return ResponseEntity.ok(response.toMap());
    }

    @PostMapping("/ready")
    public ResponseEntity<Map<String, Object>> saveMetadata(@RequestBody Map<String, Object> metadata) {
        Metadata meta = metadataService.saveMetadata(new JSONObject(metadata));
        String nodeList = meta.getChunkLocations();
        String[] parts = nodeList.split(",");
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < 3; i++){
            builder.append(dataNodeService.getDataNode(parts[i]).getIpAddress());
            builder.append(":");
            builder.append(dataNodeService.getDataNode(parts[i]).getPort());
            builder.append(",");
        }
        JSONObject response = new JSONObject();
        response.put("nodeList",builder.toString());
        response.put("fileId",meta.getFileId());
        return ResponseEntity.ok(response.toMap());
    }

    @GetMapping("/finished/{fileId}")
    public ResponseEntity<String> startReplica(@PathVariable int fileId) throws IOException {
        metadataService.startReplica(fileId,dataNodeService.getDataNodes());
        return ResponseEntity.ok("All good!");
    }


    @GetMapping("/{fileId}")
    public ResponseEntity<Metadata> getMetadata(@PathVariable Long fileId) {
        Optional<Metadata> metadata = metadataService.getMetadata(fileId);
        return metadata.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{fileName}")
    public ResponseEntity<String> deleteMetadata(@PathVariable String fileName) {
        if(!metadataService.isFileExist(fileName)){
            return ResponseEntity.badRequest().build();
        }
        metadataService.deleteMetadata(fileName,dataNodeService.getDataNodes());
        return ResponseEntity.ok("File deleted successfully!");
    }
}
