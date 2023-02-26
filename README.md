# ClientServerAndroid
Python server with Android client implementation for sending vectors data from server to client

Python server application and a small client app that runs on a mobile device (or emulator). The two
applications communicate using TCP socket. Client should enter the server ip address and press connect. Then the server will send to the client randomly gausian data vectors. These data vectors are accumulated in the client app in a matrices and save simple statistics
computed (across the “temporal” dimension) to file results.txt. 
