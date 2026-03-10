package com.github.carhosts.vpn;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.github.carhosts.vpn.util.LogUtils;
import com.github.carhosts.vpn.vservice.CarVpnService;
import com.github.carhosts.vpn.vservice.DnsChange;

import java.util.HashMap;
import java.util.Map;

/**
 * 车机 Hosts VPN 主界面
 * 支持通过代码自定义 hosts，无需修改系统文件
 */
public class VpnActivity extends Activity {

    private static final String TAG = VpnActivity.class.getSimpleName();
    private static final int VPN_REQUEST_CODE = 0x0F;

    private Button vpnToggleButton;
    private TextView statusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 创建简单 UI
        setContentView(createContentView());
        
        vpnToggleButton = findViewById(R.id.vpn_toggle_button);
        statusTextView = findViewById(R.id.status_text);
        
        vpnToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CarVpnService.isRunning()) {
                    CarVpnService.stopVpnService(VpnActivity.this);
                    updateStatus(false);
                } else {
                    startVpn();
                }
            }
        });

        // 示例：通过代码添加 hosts 条目
        setupCustomHosts();
        
        updateStatus(CarVpnService.isRunning());
    }

    /**
     * 设置自定义 hosts - 这是核心功能
     * 可以在这里添加任意 hosts 映射
     */
    private void setupCustomHosts() {
        // 方式 1: 添加单个 hosts 条目
        DnsChange.addHostEntry("127.0.0.1", "ads.example.com");
        DnsChange.addHostEntry("192.168.1.100", "local.server.com");
        
        // 方式 2: 批量添加 hosts
        Map<String, String> hostsMap = new HashMap<>();
        hostsMap.put("block.ads.com", "0.0.0.0");
        hostsMap.put("block.tracker.com", "0.0.0.0");
        hostsMap.put("api.custom.com", "10.0.0.50");
        DnsChange.addHostEntries(hostsMap);
        
        LogUtils.i(TAG, "Custom hosts configured");
    }

    private void startVpn() {
        Intent vpnIntent = VpnService.prepare(this);
        if (vpnIntent != null) {
            startActivityForResult(vpnIntent, VPN_REQUEST_CODE);
        } else {
            onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            CarVpnService.startVpnService(this);
            updateStatus(true);
        } else {
            Toast.makeText(this, "VPN permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateStatus(boolean isRunning) {
        if (isRunning) {
            vpnToggleButton.setText("Stop VPN");
            statusTextView.setText("VPN Status: Running\nHosts are being redirected");
        } else {
            vpnToggleButton.setText("Start VPN");
            statusTextView.setText("VPN Status: Stopped\nTap to start");
        }
    }

    private View createContentView() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);
        layout.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT));
        layout.setBackgroundColor(0xFF1a1a2e);

        TextView title = new TextView(this);
        title.setText("CarHosts VPN");
        title.setTextSize(24);
        title.setTextColor(0xFFFFFFFF);
        title.setGravity(android.view.Gravity.CENTER);
        title.setPadding(0, 20, 0, 20);
        layout.addView(title);

        statusTextView = new TextView(this);
        statusTextView.setText("VPN Status: Stopped");
        statusTextView.setTextSize(16);
        statusTextView.setTextColor(0xFFaaaaaa);
        statusTextView.setGravity(android.view.Gravity.CENTER);
        statusTextView.setPadding(0, 10, 0, 30);
        layout.addView(statusTextView);

        vpnToggleButton = new Button(this);
        vpnToggleButton.setText("Start VPN");
        vpnToggleButton.setTextSize(18);
        vpnToggleButton.setPadding(0, 15, 0, 15);
        android.widget.LinearLayout.LayoutParams buttonParams = 
            new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.setMargins(0, 0, 0, 20);
        vpnToggleButton.setLayoutParams(buttonParams);
        layout.addView(vpnToggleButton);

        TextView infoText = new TextView(this);
        infoText.setText("This app uses VPN to redirect DNS queries\nbased on custom hosts rules.\nNo root required.");
        infoText.setTextSize(14);
        infoText.setTextColor(0xFF888888);
        infoText.setGravity(android.view.Gravity.CENTER);
        infoText.setPadding(20, 20, 20, 20);
        layout.addView(infoText);

        return layout;
    }
}
