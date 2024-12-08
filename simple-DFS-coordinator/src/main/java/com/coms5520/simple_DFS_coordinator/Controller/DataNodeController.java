package com.coms5520.simple_DFS_coordinator.Controller;

import com.coms5520.simple_DFS_coordinator.*;
import com.coms5520.simple_DFS_coordinator.Service.DataNodeService;
import com.coms5520.simple_DFS_coordinator.Utility.DataNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collection;

@RestController
@RequestMapping("/api/dataNodes")
public class DataNodeController {

    private final DataNodeService dataNodeService;
    private int nodeId = 1;

    @Autowired
    public DataNodeController(DataNodeService dataNodeService) {
        this.dataNodeService = dataNodeService;
    }

    @GetMapping("/{port}")
    public ResponseEntity<Integer> dataNodeJoin(@PathVariable int port) {
        DataNode dataNode = new DataNode(nodeId, "localhost",port);
        nodeId++;
        return ResponseEntity.ok(dataNodeService.addDataNode(dataNode));
    }

    @DeleteMapping("/{id}")
    public int dataNodeLeave(@PathVariable int id) {
        return dataNodeService.removeDataNode(id);
    }

}
