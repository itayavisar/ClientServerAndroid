package com.example.cogntivclientv2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button connectBtn = (Button)findViewById(R.id.button); //TODO change id
        TextInputLayout ipAddressTxt = (TextInputLayout)findViewById(R.id.textInputLayout);
        connectBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String ipAddress = ipAddressTxt.getEditText().getText().toString();
                System.out.println("ITAY ip adress = "+ipAddress);
                Intent intent = new Intent(MainActivity.this, ConnectedActivity.class);
                intent.putExtra("ipAddress", ipAddress);
                startActivity(intent);
            }
        });
    }
}