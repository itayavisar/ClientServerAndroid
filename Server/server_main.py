import socket
import numpy as np
import datetime
import time
from threading import Thread
import signal


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
        self.data_size = (50, 1)

        # set statistics parameters
        self.sum_rate = 0
        self.avg_rate = 0
        self.num_sent = 0
        self.previous_sent_timestmap = datetime.datetime.now()

    def handle_sigint(self, fig, frame):
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

    def server_ctrl(self):
        print("input for server control:"
              "\nq: quit")
        data = input("->\n")
        if 'q' == data:
            self.close_server()

    def calc_statistics(self):
        return

    def run(self):
        self.alive = True
        # thread = Thread(target=self.server_ctrl)
        # thread.start()

        print(f"host is {self.host}")
        self.server_socket = socket.socket()
        server_socket = self.server_socket
        server_socket.bind((self.host, self.port))
        server_socket.listen(1)

        while self.alive:
            print("server running, waiting for connection...")
            self.socket_connection, address = server_socket.accept()  # accept new connection
            self.socket_connection.settimeout(2)
            self.client_connected = True
            print("Connection from: " + str(address))
            self.num_sent = 0
            self.sumrate = 0
            self.avg_rate = 0
            try:
                start_time = time.time()
                while self.client_connected:
                    data = np.random.normal(0, 1, self.data_size)
                    now = time.time()
                    if self.num_sent > 0:
                        avg_rate = self.num_sent / ((now - start_time) + self.EPSILON)

                        if self.sending_rate < avg_rate:
                            sleep_time = (1/self.sending_rate) - (1/avg_rate)
                            time.sleep(sleep_time)
                    self.socket_connection.send(data)
                    self.num_sent += 1

            except (ConnectionResetError):
                print("client close the connection")
            except (TimeoutError):
                print("connection timeout")
            except:
                print("connection failed")

            finally:
                self.socket_connection.close()

        thread.join()
        self.close_server()


if __name__ == '__main__':
    server = CogntivServer()
    server.run()