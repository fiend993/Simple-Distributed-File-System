package com.coms5520.simple_DFS_coordinator.Interface;

import com.coms5520.simple_DFS_coordinator.Utility.Metadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MetadataRepository extends JpaRepository<Metadata, Long> {

    Optional<Metadata> findFileByFileName(String fileName);
    void deleteFileByFileName(String fileName);
}
