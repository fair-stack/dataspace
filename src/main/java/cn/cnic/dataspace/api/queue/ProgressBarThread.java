package cn.cnic.dataspace.api.queue;

import cn.cnic.dataspace.api.util.Constants;
import cn.cnic.dataspace.api.websocket.WebSocketProcess;
import com.alibaba.fastjson.JSONObject;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class ProgressBarThread implements Runnable {

    // Current progress
    private long progress;

    // Download Grammar Total Size
    private long totalSize;

    private boolean run = true;

    private double showProgress;

    private WebSocketProcess webSocketTask;

    private String user;

    private Map<String, Object> messageMap;

    public ProgressBarThread(long totalSize, String taskId, String user, String rootId, WebSocketProcess webSocketTask) {
        this.totalSize = totalSize;
        this.progress = 0L;
        this.webSocketTask = webSocketTask;
        this.user = user;
        this.messageMap = new HashMap<>(6);
        this.messageMap.put("type", Constants.SocketType.TS_FILE);
        this.messageMap.put("taskId", taskId);
        this.messageMap.put("rootId", rootId);
        this.messageMap.put("mark", "space");
    }

    /**
     * @ param progress progress
     */
    public void updateProgress(long progress) {
        this.progress += progress;
    }

    public void finish() {
        this.run = false;
        // Close progress bar
    }

    @Override
    public void run() {
        try {
            while (this.run) {
                Thread.sleep(300);
                if (this.run) {
                    // Update progress bar
                    showProgress = this.totalSize > 0 ? ((new BigDecimal((float) this.progress / this.totalSize).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()) * 100) : 100;
                    String s = String.valueOf(showProgress);
                    messageMap.put("showProgress", (s.length() > 4 ? Double.valueOf(s.substring(0, 4)) : showProgress));
                    webSocketTask.sendMessage(user, JSONObject.toJSONString(messageMap));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
