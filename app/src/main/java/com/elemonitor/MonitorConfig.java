package com.elemonitor;

public class MonitorConfig {
    public double intervalMinutes = 2;
    public String columnName = "15min挂单量";
    public String comparison = ">";
    public double threshold = 0;
    public int cooldownSeconds = 60;
    public String targetUrl = "https://r.ele.me/dm-area-data-board/#/agency/data/monitor";
}
