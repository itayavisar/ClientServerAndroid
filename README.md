# ClientServerAndroid
Python server with Android client implementation for sending vectors data from server to client

Python server application and a small client app that runs on a mobile device (or emulator). The two
applications communicate using TCP socket. Client should enter the server ip address and press connect. Then the server will send to the client randomly gausian data vectors. These data vectors are accumulated in the client app in a matrices and save simple statistics
computed (across the “temporal” dimension) to file results.txt. 

# How to run?
1. Server:
1.1. first run the server to open the connection:
1.2. just call: $ python server_main.py

2. Client:
2.1 Open the Client project in android-studio-2022.1.1.20 or above
2.2 Launch an android emulator (tested on pixel-4a)
2.3 In the mainActicity fill the server IP (example 192.168.5.5) and presss connect. make sure to fill the ip address well
2.4 App will switch to the connection activity and will start receive vectors from server
2.5 If desired to stop the connection, press disconnect. The statistical and analytical results will be saved in the App files directory at results.txt (e.g: /data/user/0/com.example.cogntivclientv2/files/results.txt)
