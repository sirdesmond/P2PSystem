# P2P System


This is a napster style file sharing system built with scala and akka cluster

### Assumed definitions
* `Server` stores peers and their registered file paths (not actual files). 
* `Peer`  A seeder of a file that registers it's file paths with the index server  
* `Client` A leecher that wants to lookup a particular file from the index server  and download it
        from a selected peer(seeder)

For testing purposes, the files named `peer1`,`peer2` and `peer3` have the list of filenames those respective peers will seed.

Run the `make-files.sh` script to create some dummy files of 1K to 10K size. 

Place them in a directory( default is `/tmp/shared`)

Verify that the files in the shared directory match the filenames in the `peer1`,`peer2` and `peer3` files

*****
* assignable server ports: b/n `2000` and `2999` ( This socket should match the socket provided as the seed node in the `run.sh` script)
* assignable peer ports:   b/n `3000` and `3999` 
* assignable client ports  >= `4000`
* Make sure that the socket used for the server is a match with what you provide in `run.sh`.
* For multiple vms use unique IP not loopback.

*****

#### Run the following commands from the directory with the jar in different terminals
    Start the server with ./run.sh 127.0.0.1 2551 (assuming the seednode setting in run.sh is 127.0.0.1:2551)
    Start peer 1 with ./run.sh 127.0.0.1 3001 1 /tmp/shared1
    Start peer 2 with ./run.sh 127.0.0.1 3002 2 /tmp/shared2
    Start client 1 with ./run.sh 127.0.0.1 4000
    Start client 2 with ./run.sh 127.0.0.1 4001

Follow the prompt in the client terminals to lookup and download. 
 
Enjoy!!

#### v2.0
1. A backup will be created for the central point of failure ( the index server ) with data replication.
2. A distributed hash table will be substituted for the concurrent map currently being used.  return
3. Peers will register with more information so peer list will be sent to clients by priority
