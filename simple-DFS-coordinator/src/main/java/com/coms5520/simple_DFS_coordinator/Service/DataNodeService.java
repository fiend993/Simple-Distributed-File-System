package com.coms5520.simple_DFS_coordinator.Service;

import com.coms5520.simple_DFS_coordinator.Utility.DataNode;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DataNodeService {
    private final ConcurrentHashMap<String, DataNode> dataNodeList = new ConcurrentHashMap<>();

    public int addDataNode(DataNode dataNode) {
        if(dataNodeList.get("node"+dataNode.getId())==null){
            dataNodeList.put("node"+dataNode.getId(), dataNode);
        }else{
            return 0;
        }
        return dataNode.getId();
    }

    public int removeDataNode(int id) {
        if(dataNodeList.remove("node"+id) == null){
            return 0;
        }
        return 1;
    }

    public DataNode getDataNode(String id) {
        return dataNodeList.get(id);
    }

    public ConcurrentHashMap<String, DataNode> getDataNodes() {
        return dataNodeList;
    }

    public int getNumberOfDataNodes() {
        return dataNodeList.size();
    }
}
