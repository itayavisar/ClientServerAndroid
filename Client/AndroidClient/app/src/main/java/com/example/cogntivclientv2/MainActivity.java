package com.example.cogntivclientv2;

import com.google.android.material.textfield.TextInputLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.widget.Button;
import android.view.View;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    protected String ipAddressStr = null;
    TextInputLayout ipAddressTxt = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button connectBtn = (Button)findViewById(R.id.connect_button);
        Button exitBtn = (Button)findViewById(R.id.exit_button);

        ipAddressTxt = (TextInputLayout)findViewById(R.id.textInputLayout);

        exitBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finishAffinity();
            }
        });

        connectBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String ipAddress = ipAddressTxt.getEditText().getText().toString();
                ipAddressStr = ipAddress;
                if (ipAddress.isEmpty()) {
                    ipAddressTxt.setHint("Invalid ip please try again");
                    return;
                }
                Intent intent = new Intent(MainActivity.this, ConnectedActivity.class);
                intent.putExtra("ipAddress", ipAddress);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        System.out.println("[onSaveInstanceState] ipAddressStr = "+ipAddressStr);
        super.onSaveInstanceState(outState);
        String ipAddr = ipAddressTxt.getEditText().toString();
        outState.putString("ipAddress", ipAddr);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        System.out.println("[onRestoreInstanceState]");
        super.onRestoreInstanceState(savedInstanceState);
        String ipAddr = savedInstanceState.getString("ipAddress");
        System.out.println("[onRestoreInstanceState] ipAddressStr = "+ipAddressStr);
        System.out.println("[onRestoreInstanceState] ipAddr = "+ipAddr);
        ipAddressTxt.getEditText().setText(ipAddr);
    }
}