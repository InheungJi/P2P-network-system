# P2P-network-system

• Languages: Java

• Goal: to download the files from a peer who has the files that the user request to get.

• Features
 - When a peer wants to download it, checking servers which are implemented as a distributed hash table about who has the file.
 - Each server communicates by using UDP.
 - After figuring out the file’s owner, the server sends the IP address of the file's owner to the peer who wants to download.
 - Each peer communicates by using TCP based on IP address and content name.

• Explanation
 - It implemented small size of P2P server like a torrent.
 - Even though it showed some bugs, in the end, it gave me a chance to know the characteristics of UDP and TCP and how to connect each other based on a distributed hash table. Furthermore, I have got a knowledge of a comprehensive network system.


 
