package com.example.cogntivclientv2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

public class ConnectedActivity extends AppCompatActivity {

    /* network attributes*/
    Socket socket;

    // ip address of the server example: 10.100.102.15
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
    double dataAcuisitionRateMean = 0.0;
    double dataAcuisitionRateStd = 0.0;
    double[] dataSeriesMean = new double[dataVectorLen];
    double[] dataSeriesVariance = new double[dataVectorLen];
    double[] previousSeriesMean = new double[dataVectorLen];

    FileOutputStream resultsFile = null;
    DataOutputStream resultStream = null;
    final String filename = "results.txt";

    // count the current number of vectors contained in the accumulated matrix
    int numVectorsInMatrix = 0;

    final long usecInSec = 1000000;

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
                        File file = null;
                        if (null == resultsFile) {
                            file = new File(getFilesDir(), filename);
                        }
                        resultsFile = new FileOutputStream(file, true);
//                        resultsFile = openFileOutput(filename, Context.MODE_APPEND);
                        resultStream = new DataOutputStream(resultsFile);
                        // create connection
                        serverAddress = getIntent().getStringExtra("ipAddress");
                        socket = new Socket(InetAddress.getByName(serverAddress), port);
                        serverConnected = true;

                        DataInputStream dataIn = new DataInputStream(socket.getInputStream());

                        int totalSize = dataVectorLen * elementSize;
                        float[] data = new float[dataVectorLen];

                        long numReads = 0;
                        long now = getMicroSec();
                        long prevTime = now;
                        double sumRate = 0;

                        while (serverConnected) {
                            receiveData(dataIn, data, totalSize);
                            ++numReads;

                            System.out.println("ITAY DEBUG inside while after in.readLine() numReads = " + numReads);
                            now = getMicroSec();
                            if (numReads > 1) {
                                double rateAcquisition = (double)usecInSec / ((double)now - (double)prevTime);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        TextView dataAcquisitionRateTxt = (TextView)findViewById(R.id.rateTxt);
                                        String rateStr = String.format("%.2f", rateAcquisition);
                                        dataAcquisitionRateTxt.setText(rateStr);
                                    }
                                });

                            }
                            prevTime = now;
                        }

                        // server disconnected
                        dataIn.close();
                    } catch (Exception e) {
                        System.out.println("Failed to create Socket\n");
                        e.printStackTrace();
                    }
                }
            }
        };

        dataAcuisitionThread = new Thread(connectTask);
        System.out.println("ITAY DEBUG onClick before thread.start\n");
        dataAcuisitionThread.start();
        System.out.println("ITAY DEBUG onClick after thread.start\n");

        disconnectBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                if (serverConnected) {
                    serverConnected = false;
                    try {
                        dataAcuisitionThread.interrupt();
                        dataAcuisitionThread = null;

                        socket.shutdownInput();
                        socket.shutdownOutput();
                        socket.close();
                        resultStream.close();
                        resultsFile.close();

                        resetStatistics();
                    } catch (Exception e) {
                        System.out.println("Failed to close Socket\n");
                        e.printStackTrace();
                    }

                    Intent intent = new Intent(ConnectedActivity.this, MainActivity.class);
                    startActivity(intent);
                }
            }
        });
    }

    private void receiveData(DataInputStream dataIn, float[] data, int dataSize) throws IOException {
        byte[] buffer = new byte[dataSize];
        int bytesRead = 0;

        while (bytesRead < dataSize) {
            int count = dataIn.read(buffer, bytesRead, dataSize - bytesRead);
            bytesRead += count;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
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
        Arrays.fill(previousSeriesMean, 0.0);
        Arrays.fill(dataSeriesMean, 0.0);
        Arrays.fill(dataSeriesVariance, 0.0);
    }

    private void resetStatistics()
    {
        dataAcuisitionRateMean = 0;
        dataAcuisitionRateStd = 0;
        resetData();
    }

    private void updateStatistics(double[] newSample)
    {
        try {
            updateMeanSeries(newSample);
            updateVarianceSeries(newSample);
            ++numVectorsInMatrix;
            if (numAccumulateData == numVectorsInMatrix)
            {
                numVectorsInMatrix = 0;
                resetData();
            }
        } catch (java.io.IOException e) {
            System.out.println("Failed save data to file\n");
            e.printStackTrace();
        }

    }

    private void updateMeanSeries(double[] newSample) throws IOException {
        assert(newSample.length == dataVectorLen);
        if (numAccumulateData - 1 == numVectorsInMatrix)
            resultStream.writeChars("temporal mean: ");
        for (int i = 0; i < newSample.length; ++i)
        {
            // save previous mean to calculate the accumulated variance
            previousSeriesMean[i] = dataSeriesMean[i];
            dataSeriesMean[i] *= numVectorsInMatrix;
            dataSeriesMean[i] += newSample[i];
            dataSeriesMean[i] /= (numVectorsInMatrix + 1);
            if (numAccumulateData - 1 == numVectorsInMatrix)
                resultStream.writeChars(", "+dataSeriesMean[i]);

        }
        if (numAccumulateData - 1 == numVectorsInMatrix)
            resultStream.writeChars("\n");
    }

    private void updateVarianceSeries(double[] newSample)
    {
        assert(newSample.length == dataVectorLen);
        for (int i = 0; i < newSample.length; ++i)
        {
            dataSeriesVariance[i] *= numVectorsInMatrix;
            dataSeriesVariance[i] += Math.pow((newSample[i] - previousSeriesMean[i]), 2);
            dataSeriesVariance[i] /= (numVectorsInMatrix + 1);
        }
    }
}