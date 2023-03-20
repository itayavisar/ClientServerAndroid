package com.example.cogntivclientv2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ConnectedActivity extends AppCompatActivity {
    /* network attributes*/
    Socket socket;

    // ip address of the server example: 10.100.105.15
    String serverAddress = null;
    int port = 5557;
    boolean serverConnected = false;
    // thread responsible to acquire the input data from the socket
    Thread dataAcuisitionThread = null;

    /* input data attributes*/
    final int dataVectorLen = 50; //TODO should be requested from server
    final int elementSize = 4; //TODO should be requested from server
    final int numAccumulateData = 100; //TODO should be requested from server

    /* statistics attributes*/
    Statistics dataAcquisitionStatistics = new Statistics();
    float[] dataSeriesMean = new float[dataVectorLen];
    float[] dataSeriesStd = new float[dataVectorLen];
    float[] previousSeriesMean = new float[dataVectorLen];

    /* IO attributes */
    PrintWriter resultsFileWriter = null;
    final String filename = "results.txt";
    final float[] dataStopVector = new float[dataVectorLen];
    Thread resultsWriterThread = null;

    // maximum size of the buffer to write to file. Avoid from buffering high memory consumption.
    static final int maxSizeFileBuffer = 10000;
    static BlockingQueue<float[]> sharedDataBufferMean = new ArrayBlockingQueue<float[]>(maxSizeFileBuffer);
    static BlockingQueue<float[]> sharedDataBufferStd = new ArrayBlockingQueue<float[]>(maxSizeFileBuffer);
    static BlockingQueue<Statistics> sharedDataBufferDataAcquisitionStatistics = new ArrayBlockingQueue<Statistics>(maxSizeFileBuffer);


    // count the current number of vectors contained in the accumulated matrix
    int numVectorsInMatrix = 0;

    final long usecInSec = 1000000;

    // count the number of received data in a session
    long numReceived = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected);
        Button disconnectBtn = (Button)findViewById(R.id.disconnectBtn); //TODO change id
        Runnable connectTask = new Runnable() {
            @Override
            public void run() {
                {
                    // already connected, do nothing.
                    if (serverConnected)
                        return;

                    // reset statistics before new connection started
                    resetStatistics();

                    try {
                        /* establish connection and intialization of metadata */

                        // create connection
                        serverAddress = getIntent().getStringExtra("ipAddress");
                        final int connectionTimeoutMiliSec = 5000;
                        socket = new Socket();

                        // trying to connect to server with 5 seconds timeout
                        socket.connect(new InetSocketAddress(serverAddress, port), connectionTimeoutMiliSec);
                        serverConnected = true;

                        DataInputStream dataIn = new DataInputStream(socket.getInputStream());
                        numReceived = 0;

                        int totalSize = dataVectorLen * elementSize;
                        float[] data = new float[dataVectorLen];

                        long now = getMicroSec();
                        long startTime = now;
                        while (serverConnected) {
                            receiveData(dataIn, data, totalSize);
                            ++numReceived;

                            // update Data acquisition statistics after filling acquire each matrix
                            if (0 == ((numReceived + 1) % numAccumulateData)) {
                                now = getMicroSec();
                                float rate = (float) (numAccumulateData * usecInSec) / ((float) now - (float) startTime);
                                dataAcquisitionStatistics.addSample(rate);
                                startTime = now;
                            }

                            updateStatistics(data);
                            //System.out.println("[HandleConnection] number of read vectors = " + numReceived); TODO - use for debug

                            // update rate on screen
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TextView dataAcquisitionRateTxt = (TextView) findViewById(R.id.rateTxt);
                                    String rateStr = String.format("%.2f", dataAcquisitionStatistics.getSample());
                                    dataAcquisitionRateTxt.setText(rateStr);
                                }
                            });
                        }

                        // server disconnected
                        dataIn.close();
                    } catch (java.net.SocketTimeoutException e)
                    {
                        System.out.println("Failed to connect to host " + serverAddress);
                        e.printStackTrace();
                    } catch (java.net.UnknownHostException e) {
                        System.out.println("Failed to create Socket unknown host "+ serverAddress + "- reach timeout");
                        e.printStackTrace();
                    } catch (Exception e)
                    {
                        System.out.println(e.getMessage());
                        e.printStackTrace();
                    }
                    finally {
                        Intent intent = new Intent();
                        setResult(RESULT_OK, intent);
                        finish();
                    }

                }
            }
        };

        Runnable resultsWriterTask =  new Runnable() {
            @Override
            public void run() {
                try {
                    // initialize results.txt file
                    File file = null;
                    if (null == resultsFileWriter) {
                        file = new File(getFilesDir(), filename);
                    }

                    resultsFileWriter = new PrintWriter(file);
                    while (true) {
                        // stop condition for writing task - if data pushed to mean buffer
                        // is the stop vector

                        float[] data = sharedDataBufferMean.take();
                        if (dataStopVector == data)
                        {
                            System.out.println("all data written successfully in path: " + getFilesDir() + filename);
                            sharedDataBufferMean.clear();
                            sharedDataBufferStd.clear();
                            sharedDataBufferDataAcquisitionStatistics.clear();
                            break;
                        }

                        /// print to result file the matrix statistics
                        resultsFileWriter.println("============= matrix " + numReceived / numAccumulateData + " statistics ================");
                        // write data mean to file
                        writeStatisticToFile(data, dataVectorLen, "mean vector");
                        // write data std to file
                        data = sharedDataBufferStd.take();
                        writeStatisticToFile(data, dataVectorLen, "std vector");

                        // write data acuisition statistics to file
                        Statistics dataAquisitionStatistics = sharedDataBufferDataAcquisitionStatistics.take();
                        writeDataAquisitionStatisticsToFile(dataAquisitionStatistics);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    resultsFileWriter.close();
                }
            }
        };

        dataAcuisitionThread = new Thread(connectTask);
        resultsWriterThread = new Thread(resultsWriterTask);
        dataAcuisitionThread.start();
        resultsWriterThread.start();

        // handle disconnection and return to the MainActivity
        disconnectBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                if (serverConnected) {
                    serverConnected = false;
                    try {
                        // indicate to IO thread to end
                        sharedDataBufferMean.put(dataStopVector);
                        sharedDataBufferStd.put(dataStopVector);
                        dataAcuisitionThread.interrupt();
                        dataAcuisitionThread = null;
                        if (socket.isConnected()) {
                            socket.shutdownInput();
                            socket.shutdownOutput();
                            socket.close();
                        }

                        resetStatistics();
                    } catch (Exception e) {
                        System.out.println("Failed to close Socket\n");
                        e.printStackTrace();
                    }

                    Intent intent = new Intent(ConnectedActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }

    private void writeStatisticToFile(float[] vector, int vectorLen, String vectorName)
    {
        resultsFileWriter.print(vectorName + ": ");
        for (int i = 0; i < vectorLen; ++i) {
            resultsFileWriter.print(vector[i] + ", ");
        }
        resultsFileWriter.println(" ");
        resultsFileWriter.flush();
    }

    private void writeDataAquisitionStatisticsToFile(Statistics dataAquisitionStatistics)
    {
        resultsFileWriter.println("data acquisition rate: " + dataAquisitionStatistics.getSample());
        resultsFileWriter.println("data acquisition rate mean: " + dataAquisitionStatistics.getMean());
        resultsFileWriter.println("data acquisition rate std: " + dataAquisitionStatistics.getStd());
    }
    private void receiveData(DataInputStream dataIn, float[] data, int dataSize) throws IOException {
        byte[] buffer = new byte[dataSize];
        int bytesRead = 0;

        while (bytesRead < dataSize) {
            int count = dataIn.read(buffer, bytesRead, dataSize - bytesRead);
            bytesRead += count;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        // data received as little endian and need to reorder bytes
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        floatBuffer.get(data);
    }

    private long getMicroSec()
    {
        return System.nanoTime() / 1000;
    }

    private void resetData()
    {
        Arrays.fill(previousSeriesMean, 0.0f);
        Arrays.fill(dataSeriesMean, 0.0f);
        Arrays.fill(dataSeriesStd, 0.0f);
    }

    private void resetStatistics()
    {
        resetData();
        dataAcquisitionStatistics.reset();
    }

    private void updateStatistics(float[] newSample)
    {
        try {
            updateMeanSeries(newSample);
            updateStdSeries(newSample);
            ++numVectorsInMatrix;
            if (numAccumulateData == numVectorsInMatrix)
            {
                sharedDataBufferDataAcquisitionStatistics.put(new Statistics(dataAcquisitionStatistics));
                numVectorsInMatrix = 0;
                resetData();
            }
        } catch (java.io.IOException e) {
            System.out.println("Failed save data to file");
            e.printStackTrace();
        }
        catch (Exception e)
        {
            System.out.println("Failed to update Statistics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateMeanSeries(float[] newSample) throws IOException {
        assert(newSample.length == dataVectorLen);

        for (int i = 0; i < newSample.length; ++i)
        {
            // save previous mean to calculate the accumulated std
            previousSeriesMean[i] = dataSeriesMean[i];
            dataSeriesMean[i] = Statistics.calcMean(previousSeriesMean[i], numVectorsInMatrix, newSample[i]);
        }
        try {
            if ((0 < sharedDataBufferMean.remainingCapacity()) && ((numAccumulateData - 1) == numVectorsInMatrix))
            {
                // write data to file only if there is enough place in the buffer. Otherwise,
                // drop if the Buffer is too big to avoid stack overflow and memory high consumption
                sharedDataBufferMean.put(dataSeriesMean.clone());
            }
        } catch (Exception e) {
            System.out.println("Couldn't put data mean to buffer");
            e.printStackTrace();
        }
    }

    private void updateStdSeries(float[] newSample)
    {
        assert(newSample.length == dataVectorLen);
        for (int i = 0; i < newSample.length; ++i)
        {
            dataSeriesStd[i] = Statistics.calcStd(dataSeriesStd[i], previousSeriesMean[i], dataSeriesMean[i], numVectorsInMatrix, newSample[i]);
        }

        try {
            if (0 < sharedDataBufferStd.remainingCapacity() && ((numAccumulateData -1 ) == numVectorsInMatrix))
            {
                // drop if the Buffer is too big to avoid stack overflow and memory high consumption
                sharedDataBufferStd.put(dataSeriesStd.clone());
            }
        } catch (Exception e) {
            System.out.println("Couldn't put data Std to buffer");
            e.printStackTrace();
        }
    }
}