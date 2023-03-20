# ClientServerAndroid
Python server with Android client implementation for sending data vectors from server to client

Python server application and a small client app that runs on a mobile device (or emulator). The two
applications communicate using TCP socket. Client should enter the server ip address and press connect. Then the server will send to the client randomly gausian data vectors. These data vectors are accumulated in the client app in a matrices and save simple statistics
computed (across the “temporal” dimension) to file results.txt. 

# How to run?
1. Server:
* first run the server to open the connection:
* run server script: $ python server_main.py

2. Client:
* Open the Client project in android-studio-2022.1.1.20 or above
* Launch an android emulator (tested on pixel-4a)
* In the mainActicity fill the server IP (example 192.168.5.5) and presss connect. make sure to fill the ip address well
* App will switch to the connection activity and will start receive vectors from server
* If desired to stop the connection, press disconnect. The statistical and analytical results will be saved in the App files directory at results.txt (e.g: /data/user/0/com.example.cogntivclientv2/files/results.txt)
* to exit from the application press exit from the main activity, or disconnect from the connection activity and exit again.
