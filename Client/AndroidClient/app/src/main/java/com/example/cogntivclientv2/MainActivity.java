package com.example.cogntivclientv2;

import com.google.android.material.textfield.TextInputLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.widget.Button;
import android.view.View;
import android.os.Bundle;

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
}