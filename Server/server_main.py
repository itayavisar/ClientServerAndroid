from threading import Thread
import numpy as np
import datetime
import socket
import signal
import time

class CogntivServer:
    def __init__(self):
        # constants
        self.USEC = 1000000.0
        self.EPSILON = 0.00001

        # register the signal handler
        signal.signal(signal.SIGINT, self.handle_sigint)

        # set default running parameters
        self.alive = False
        self.client_connected = False

        # set network parameters
        self.port = 5557  # initiate port no above 1024
        self.host = socket.gethostname()
        self.socket_connection = None
        self.server_socket = None
        self.sending_rate = 1000.0
        self.data_length = 50

        # set statistics parameters
        self.sum_rate = 0
        self.avg_rate = 0
        self.num_sent = 0
        self.previous_sent_timestmap = datetime.datetime.now()

    def handle_sigint(self, sig, frame):
        print('Caught SIGINT, shutting down server...')
        if self.socket_connection:
            self.socket_connection.close()
        self.socket_connection = None

    def close_server(self):
        self.client_connected = False
        self.alive = False

        if self.socket_connection:
            self.socket_connection.close()
        self.socket_connection = None

    def calc_statistics(self):
        return

    def openSocket(self):
        self.server_socket = socket.socket()
        self.server_socket.bind((self.host, self.port))
        self.server_socket.listen(1)
        self.alive = True
    def handleConnection(self):
        while self.alive:
            print("server running, waiting for connection...")
            self.socket_connection, address = self.server_socket.accept()  # accept new connection
            self.socket_connection.settimeout(2)
            self.client_connected = True
            print("Connection from: " + str(address))

            self.num_sent = 0
            self.sumrate = 0
            self.avg_rate = 0
            try:
                start_time = time.time()
                while self.client_connected:
                    data = np.random.normal(0, 1, self.data_length).astype(np.float32)
                    now = time.time()
                    if self.num_sent > 0:
                        avg_rate = self.num_sent / ((now - start_time) + self.EPSILON)

                        if self.sending_rate < avg_rate:
                            sleep_time = (1/self.sending_rate) - (1/avg_rate)
                            time.sleep(sleep_time)

                    self.socket_connection.send(data)
                    self.num_sent += 1

            except OSError as e:
                if str(e) == 'Bad file descriptor':
                    print("The server socket was closed, exit the loop")
            except (ConnectionResetError):
                print("client close the connection")
            except (TimeoutError):
                print("connection timeout")
            except:
                print("connection failed")

    def run(self):
        self.openSocket()
        thread = Thread(target=self.handleConnection())
        thread.start()
        thread.join()
        self.close_server()


if __name__ == '__main__':
    server = CogntivServer()
    server.run()