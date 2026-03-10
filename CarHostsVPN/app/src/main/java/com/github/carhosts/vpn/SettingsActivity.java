package com.github.carhosts.vpn;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView text = new TextView(this);
        text.setText("Settings\n\nConfigure hosts programmatically in VpnActivity.java");
        text.setTextSize(18);
        text.setPadding(30, 30, 30, 30);
        setContentView(text);
    }
}
