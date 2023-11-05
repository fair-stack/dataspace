package cn.cnic.dataspace.api.ftp.authen;

import cn.cnic.dataspace.api.ftp.minimalftp.FTPConnection;
import cn.cnic.dataspace.api.ftp.minimalftp.Utils;
import cn.cnic.dataspace.api.ftp.minimalftp.api.IFTPListener;
import lombok.extern.slf4j.Slf4j;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * @author Guilherme Chaguri
 */
@Slf4j
public class CustomServer implements IFTPListener {

    @Override
    public void onConnected(FTPConnection con) {
        Socket socket = con.getSocket();
        InetAddress inetAddress = socket.getInetAddress();
        String hostAddress = inetAddress.getHostAddress();
        log.info("IP : " + hostAddress + " -> onConnected");
        // Creates our command handler
        CommandHandler handler = new CommandHandler(con);
        // Register our custom command
        con.registerCommand("CUSTOM", "CUSTOM <string>", handler::customCommand);
    }

    @Override
    public void onDisconnected(FTPConnection con) {
        Socket socket = con.getSocket();
        InetAddress inetAddress = socket.getInetAddress();
        String hostAddress = inetAddress.getHostAddress();
        List<Integer> integers = Utils.serPort.get(hostAddress);
        int i = 0;
        int b = 0;
        if (null != integers) {
            for (Integer integer : integers) {
                if (Utils.serMap.containsKey(integer)) {
                    ServerSocket serverSocket = Utils.serMap.get(integer);
                    if (null != serverSocket) {
                        try {
                            serverSocket.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            i++;
                            Utils.serMap.remove(integer);
                        }
                    }
                }
            }
            Utils.serPort.remove(hostAddress);
            b = integers.size();
        }
        log.info("IP : " + hostAddress + " -> onDisconnected");
        log.info("The release of the port {} " + b + " close :" + i);
    }
}
