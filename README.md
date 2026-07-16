# Simple-Distributed-File-System

# Simple Distributed File System

An individual distributed file system built with Java, Spring Boot, PostgreSQL,
REST APIs, TCP sockets, multithreading, file chunking, replication, and
SHA-256 integrity validation.

I designed and implemented the complete system, including the command-line
client, coordinator service, metadata database layer, data-node server, file
transfer protocol, chunk reconstruction, replication, and deletion workflow.

## Overview

The Simple Distributed File System stores files across multiple independent
data-node processes.

A central coordinator maintains file metadata and data-node locations, while
the client transfers file contents directly to and from the data nodes through
TCP sockets. PostgreSQL stores persistent metadata such as file names, file
sizes, chunk checksums, and chunk locations.

The current implementation uses six data nodes:

- Three nodes store the primary file chunks
- Three nodes store replicated copies of those chunks

The client can explicitly retrieve either the primary copy or the replica copy.

## Features

### File operations

- List files stored in the distributed file system
- Upload files from the local machine
- Split uploaded files into chunks
- Download and reconstruct files
- Retrieve either primary or replica chunks
- Delete files and associated metadata
- Reject duplicate file names

### Distributed storage

- Store file chunks across multiple data-node processes
- Register data nodes dynamically with the coordinator
- Assign each data node an available TCP port
- Replicate primary chunks onto separate data nodes
- Maintain primary and replica node locations
- Transfer file data directly between clients and data nodes

### Data integrity

- Generate a SHA-256 checksum for every file chunk
- Validate each chunk after it reaches a data node
- Delete a received chunk when checksum validation fails
- Preserve checksum information in the metadata database
- Reassemble downloaded chunks into the original file

### Concurrency

- Accept multiple TCP connections at each data node
- Process file operations with a fixed thread pool
- Maintain thread-safe in-memory node and checksum data using
  `ConcurrentHashMap`

## Architecture

```text
                              REST API
                     ┌────────────────────────┐
                     │                        │
┌────────────────┐   │   ┌────────────────────▼────────────┐
│  Command-Line  │───┼──▶│          Coordinator            │
│     Client     │   │   │                                 │
│                │   │   │  Spring Boot REST API           │
│ LIST           │   │   │  Data-node registration         │
│ UPLOAD         │   │   │  Chunk-location management      │
│ DOWNLOAD       │   │   │  Replication coordination       │
│ DELETE         │   │   └───────────────┬─────────────────┘
└───────┬────────┘   │                   │
        │            │                   │ JPA
        │ TCP        │            ┌──────▼───────┐
        │            │            │  PostgreSQL  │
        │            │            │  Metadata DB │
        │            │            └──────────────┘
        │
        ├──────────────── Primary chunks ────────────────┐
        │                                                │
┌───────▼──────┐  ┌──────────────┐  ┌──────────────┐    │
│ Data Node 1  │  │ Data Node 2  │  │ Data Node 3  │    │
│ Primary      │  │ Primary      │  │ Primary      │    │
└───────┬──────┘  └──────┬───────┘  └──────┬───────┘    │
        │                │                 │              │
        │ replication    │ replication     │ replication  │
        ▼                ▼                 ▼              │
┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│ Data Node 4  │  │ Data Node 5  │  │ Data Node 6  │    │
│ Replica      │  │ Replica      │  │ Replica      │    │
└──────────────┘  └──────────────┘  └──────────────┘    │
```

## System Components

### Command-line client

The Java client provides the user-facing interface and supports:

```text
LIST
UPLOAD <file-path>
DOWNLOAD <file-name> false
DOWNLOAD <file-name> true
DELETE <file-name>
```

The final Boolean value in `DOWNLOAD` controls which copy is retrieved:

- `false` retrieves the primary chunks
- `true` retrieves the replica chunks

The client is responsible for:

- Communicating with the coordinator through REST requests
- Splitting files into chunks
- Computing SHA-256 checksums
- Sending chunks directly to data nodes through TCP
- Downloading chunks from the assigned nodes
- Reconstructing the original file

### Coordinator

The coordinator is a Spring Boot application that manages the system's control
plane.

Its responsibilities include:

- Registering and tracking active data nodes
- Checking for duplicate file names
- Determining the number of chunks for an upload
- Assigning chunks to data nodes
- Returning node locations to the client
- Saving and retrieving file metadata
- Coordinating replication
- Coordinating distributed deletion

The coordinator manages metadata but does not carry the file contents during
normal client uploads and downloads.

### Data nodes

Each data node:

- Opens an available TCP port
- Registers its port with the coordinator
- Stores chunks in its local file directory
- Handles upload, download, validation, replication, and deletion requests
- Calculates SHA-256 checksums for locally stored chunks
- Processes concurrent client connections through a thread pool

### PostgreSQL metadata database

PostgreSQL stores information required to locate and validate files, including:

- File ID
- File name
- Original file size
- Number of chunks
- Chunk locations
- Chunk checksums

## Upload Workflow

```text
1. The client sends the file name to the coordinator.

2. The coordinator checks whether the file already exists and determines
   the required number of chunks.

3. The client splits the file and generates a SHA-256 checksum for each
   chunk.

4. The client sends the file metadata and checksums to the coordinator.

5. The coordinator stores the metadata and returns the assigned primary
   data-node locations.

6. The client transfers each chunk directly to its assigned data node
   using TCP sockets.

7. Each data node calculates the received chunk's checksum and compares
   it with the expected value.

8. After the primary upload is complete, the coordinator instructs the
   primary nodes to create replicas on the replica nodes.
```

## Download Workflow

```text
1. The client requests either the primary or replica version of a file.

2. The coordinator verifies that the file exists.

3. The coordinator returns the corresponding data-node addresses.

4. The client connects directly to each data node through TCP.

5. The client downloads the individual chunks.

6. The client combines the chunks in order to reconstruct the original
   file.
```

## Delete Workflow

```text
1. The client sends a delete request to the coordinator.

2. The coordinator confirms that the file exists.

3. The coordinator contacts the relevant primary and replica data nodes.

4. Each data node deletes its local chunk.

5. The coordinator removes the file metadata from PostgreSQL.
```

## Technology Stack

| Category | Technology |
|---|---|
| Primary language | Java 17 |
| Coordinator | Spring Boot 3.4 |
| Database | PostgreSQL 15 |
| Persistence | Spring Data JPA / Hibernate |
| Control communication | REST APIs and JSON |
| File transfer | TCP sockets |
| Concurrency | ExecutorService and ConcurrentHashMap |
| Data integrity | SHA-256 |
| Build tool | Maven |

## Project Structure

```text
Simple-Distributed-File-System/
├── simple-DFS-client/
│   ├── src/main/java/
│   │   └── com/coms5520/simple_DFS_client/
│   │       ├── Client.java
│   │       └── FileHandler.java
│   └── pom.xml
│
├── simple-DFS-coordinator/
│   ├── src/main/java/
│   │   └── com/coms5520/simple_DFS_coordinator/
│   │       ├── Config/
│   │       ├── Controller/
│   │       ├── Interface/
│   │       ├── Service/
│   │       ├── Utility/
│   │       └── SimpleDfsCoordinatorApplication.java
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
│
├── simple-DFS-dataNode/
│   ├── src/main/java/
│   │   └── com/coms5520/simple_DFS_dataNode/
│   │       ├── DataNode.java
│   │       └── FileHandler.java
│   └── pom.xml
│
└── README.md
```

## Running the Project

### Requirements

- Java 17
- Maven
- PostgreSQL 15
- Six terminal windows for the data-node processes
- Additional terminals for the coordinator and client

### 1. Create the PostgreSQL database

```sql
CREATE DATABASE sdfsmeta;
```

Set the following environment variables for your PostgreSQL installation:

```text
SDFS_DB_USERNAME
SDFS_DB_PASSWORD
```

The default database URL is:

```text
jdbc:postgresql://localhost:5432/sdfsmeta
```

You can override it with:

```text
SDFS_DB_URL
```

### 2. Start the coordinator

macOS or Linux:

```bash
cd simple-DFS-coordinator
./mvnw spring-boot:run
```

Windows:

```powershell
cd simple-DFS-coordinator
.\mvnw.cmd spring-boot:run
```

The coordinator runs at:

```text
http://localhost:8080
```

### 3. Start six data nodes

Open six separate terminals and run the following command in each one:

```bash
cd simple-DFS-dataNode

mvn compile exec:java \
  -Dexec.mainClass="com.coms5520.simple_DFS_dataNode.DataNode"
```

Each data node selects an available TCP port and registers itself with the
coordinator.

Start the nodes in order so they receive IDs `1` through `6`.

### 4. Start the client

```bash
cd simple-DFS-client

mvn compile exec:java \
  -Dexec.mainClass="com.coms5520.simple_DFS_client.Client"
```

## Example Commands

List all files:

```text
LIST
```

Upload a file:

```text
UPLOAD testFile.jpg
```

Download from the primary nodes:

```text
DOWNLOAD testFile.jpg false
```

Download from the replica nodes:

```text
DOWNLOAD testFile.jpg true
```

Delete the file:

```text
DELETE testFile.jpg
```

## Design Decisions

### Separate control and data paths

The coordinator manages metadata and node selection through REST APIs, while
file contents move directly between clients and data nodes over TCP. This
keeps the coordinator from becoming the main file-transfer bottleneck.

### Chunk-level integrity validation

Each chunk is assigned a SHA-256 checksum before it is uploaded. The receiving
data node recalculates the checksum and deletes the chunk when validation
fails.

### Thread-per-task data-node handling

Each data node uses an `ExecutorService` to process multiple socket
connections concurrently rather than handling every client sequentially.

### Separate primary and replica nodes

Primary and replica chunks are stored on different node processes. The client
can explicitly verify the replica path by requesting the replica version of a
file.

## Current Limitations

This project is an educational distributed-systems implementation rather than
a production file-storage platform.

Current limitations include:

- The system is configured for local-machine execution
- The current node-selection logic expects six data nodes
- The coordinator is a single point of failure
- Replica retrieval is selected manually rather than through automatic failover
- Data-node membership is stored in memory
- Communication is not encrypted
- The command parser does not support file paths containing spaces
- There is no user authentication or authorization
- There is no automatic rebalancing when a node joins or leaves

## Future Improvements

- Automatic failover from primary chunks to replicas
- Heartbeats and data-node health monitoring
- Dynamic chunk placement for any number of nodes
- Consistent hashing or another scalable placement strategy
- Automatic re-replication after node failure
- TLS encryption for REST and TCP communication
- Containerized deployment with Docker Compose
- Integration and failure-recovery tests
- Streaming support for larger files
- A web-based monitoring dashboard

## My Contribution

I independently designed and implemented the complete system:

- Command-line client
- File splitting and reconstruction
- TCP file-transfer protocol
- Spring Boot coordinator
- REST endpoints
- PostgreSQL metadata model and repository
- Data-node registration
- Concurrent data-node server
- SHA-256 validation
- Primary and replica storage workflows
- Distributed deletion logic
- System integration and debugging

## Author

**Charles Lin**

Computer Science graduate focused on Java, backend engineering, distributed
systems, and software architecture.
