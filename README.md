# PubSub

Distributed system project on PubSub system using Java RMI and UDP.

The project comprises of: clients, group servers, and a registry-server.The registry stores information about
the group server(s). To obtain the existing group server list, GetList is called, via UDP. 

The client will first contact the Registry Server to get a list of existing group servers. The client calls Join
on one of the group servers and then calls Subscribe and Publish on the server. Clients
communicate to their server by means of RPC/RMI. The group server determines the matching set of clients
for a published article, and then propagates the article to each client via UDP.
The server allows at most MAXCLIENT clients at any one time. A client may leave a server at any time, but it is assumed they notify the server first.
The group servers begin by registering themselves with the registry server. UDP is used to Register to the registry server, passing to it (IP, Port, and RPC or RMI). The server may also Deregister if it wishes to go “off-line”. The main functionality of the server is to support PubSub with multiple clients, including: handling client’s requests (like join, subscribe etc. ) and sending the published articles to matched clients. 
To check whether the group server is up, the client pings the group server(s) periodically via RPC. A failed RPC/RMI is detected and an error is returned by the RPC/RMI system, in which case the client knows that the server is down. Then the client asks the
registry server for a list of existing servers and re-join to one of them.
On the registry server side, it also sends a ‘heartbeat’ string to the group server(s) via UDP and waits for a response. If there is no response, then it will assume that the server is down and remove it from the existing server list.
