package com.github.carhosts.vpn.vservice;

import android.util.Log;
import org.xbill.DNS.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DnsChange {
    private static final String TAG = DnsChange.class.getSimpleName();
    
    // 静态 hosts 映射，支持代码自定义
    private static ConcurrentHashMap<String, String> domainsIpMaps4 = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, String> domainsIpMaps6 = new ConcurrentHashMap<>();

    /**
     * 通过代码添加 hosts 条目
     */
    public static void addHostEntry(String ip, String domain) {
        if (ip.contains(":")) {
            domainsIpMaps6.put(normalizeDomain(domain), ip);
        } else {
            domainsIpMaps6.put(normalizeDomain(domain), ip);
        }
        Log.i(TAG, "Added host entry: " + domain + " -> " + ip);
    }

    /**
     * 通过代码批量添加 hosts 条目
     */
    public static void addHostEntries(Map<String, String> hostsMap) {
        for (Map.Entry<String, String> entry : hostsMap.entrySet()) {
            addHostEntry(entry.getValue(), entry.getKey());
        }
    }

    /**
     * 清除所有自定义 hosts
     */
    public static void clearHosts() {
        domainsIpMaps4.clear();
        domainsIpMaps6.clear();
        Log.i(TAG, "Cleared all host entries");
    }

    /**
     * 从输入流加载 hosts 文件
     */
    public static int loadHostsFromFile(InputStream inputStream) {
        String strComment = "#";
        String hostPatternStr = "^\\s*(" + strComment + "?)\\s*(\\S*)\\s*([^" + strComment + "]*)" + strComment + "?(.*)$";
        Pattern hostPattern = Pattern.compile(hostPatternStr);
        
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            int count = 0;
            
            while ((line = reader.readLine()) != null) {
                if (line.length() > 1000 || line.startsWith(strComment)) continue;
                
                Matcher matcher = hostPattern.matcher(line);
                if (matcher.find()) {
                    String ip = matcher.group(2).trim();
                    try {
                        Address.getByAddress(ip);
                    } catch (Exception e) {
                        continue;
                    }
                    
                    String domain = matcher.group(3).trim();
                    if (ip.contains(":")) {
                        domainsIpMaps6.put(normalizeDomain(domain), ip);
                    } else {
                        domainsIpMaps4.put(normalizeDomain(domain), ip);
                    }
                    count++;
                }
            }
            
            reader.close();
            inputStream.close();
            Log.i(TAG, "Loaded " + count + " host entries. IPv4: " + domainsIpMaps4.size() + ", IPv6: " + domainsIpMaps6.size());
            return count;
        } catch (IOException e) {
            Log.e(TAG, "Error loading hosts file", e);
            return 0;
        }
    }

    /**
     * 处理 DNS 查询包，如果匹配 hosts 则返回伪造的响应
     */
    public static ByteBuffer handleDnsPacket(Packet packet) {
        if (domainsIpMaps4.isEmpty() && domainsIpMaps6.isEmpty()) {
            Log.d(TAG, "No host entries configured");
            return null;
        }
        
        try {
            ByteBuffer packetBuffer = packet.backingBuffer;
            packetBuffer.mark();
            byte[] tmpBytes = new byte[packetBuffer.remaining()];
            packetBuffer.get(tmpBytes);
            packetBuffer.reset();
            
            Message message = new Message(tmpBytes);
            org.xbill.DNS.Record question = message.getQuestion();
            int type = question.getType();
            
            ConcurrentHashMap<String, String> domainMap;
            if (type == Type.A) {
                domainMap = domainsIpMaps4;
            } else if (type == Type.AAAA) {
                domainMap = domainsIpMaps6;
            } else {
                return null;
            }
            
            Name queryDomain = message.getQuestion().getName();
            String queryString = queryDomain.toString();
            
            Log.d(TAG, "DNS query: " + type + " : " + queryString);
            
            // 精确匹配
            if (!domainMap.containsKey(queryString)) {
                // 通配符匹配
                queryString = "." + queryString;
                int j = 0;
                while (true) {
                    int i = queryString.indexOf(".", j);
                    if (i == -1) {
                        return null;
                    }
                    String str = queryString.substring(i);
                    if (".".equals(str) || "".equals(str)) {
                        return null;
                    }
                    if (domainMap.containsKey(str)) {
                        queryString = str;
                        break;
                    }
                    j = i + 1;
                }
            }
            
            InetAddress address = Address.getByAddress(domainMap.get(queryString));
            org.xbill.DNS.Record record;
            if (type == Type.A) {
                record = new ARecord(queryDomain, 1, 86400, address);
            } else {
                record = new AAAARecord(queryDomain, 1, 86400, address);
            }
            
            message.addRecord(record, 1);
            message.getHeader().setFlag(Flags.QR);
            
            packetBuffer.limit(packetBuffer.capacity());
            packetBuffer.put(message.toWire());
            packetBuffer.limit(packetBuffer.position());
            packetBuffer.reset();
            
            packet.swapSourceAndDestination();
            packet.updateUDPBuffer(packetBuffer, packetBuffer.remaining());
            packetBuffer.position(packetBuffer.limit());
            
            Log.i(TAG, "DNS hijacked: " + question.getType() + " : " + queryDomain.toString() + " -> " + address.getHostAddress());
            return packetBuffer;
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling DNS packet", e);
            return null;
        }
    }

    private static String normalizeDomain(String domain) {
        if (domain == null) return null;
        domain = domain.trim().toLowerCase();
        if (!domain.endsWith(".")) {
            domain = domain + ".";
        }
        return domain;
    }
}
