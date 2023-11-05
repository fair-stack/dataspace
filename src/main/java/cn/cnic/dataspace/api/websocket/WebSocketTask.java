// package cn.cnic.dataspace.api.websocket;
// 
// import java.io.IOException;
// import java.util.Map;
// import java.util.concurrent.ConcurrentHashMap;
// 
// import javax.websocket.OnClose;
// import javax.websocket.OnError;
// import javax.websocket.OnMessage;
// import javax.websocket.OnOpen;
// import javax.websocket.Session;
// import javax.websocket.server.PathParam;
// import javax.websocket.server.ServerEndpoint;
// 
// import com.alibaba.fastjson.JSONObject;
// 
// 
// 
// @ServerEndpoint("/webSocket/{username}")
// public class WebSocketTask {
// 
// private static int onlineCount = 0;
// private static Map<String, WebSocketTask> clients = new ConcurrentHashMap<String, WebSocketTask>();
// private Session session;
// private String username;
// 
// @OnOpen
// public void onOpen(@PathParam("username") String username, Session session) throws IOException {
// 
// this.username = username;
// this.session = session;
// 
// addOnlineCount();
// clients.put(username, this);
// System. out. println ("connected");
// }
// 
// @OnClose
// public void onClose() throws IOException {
// clients.remove(username);
// subOnlineCount();
// }
// 
// @OnMessage
// public void onMessage(String message) throws IOException {
// JSONObject jsonTo = JSONObject.parseObject(message);
// String mes = (String) jsonTo.get("message");
// if (!jsonTo.get("To").equals("All")){
// sendMessageTo(mes, jsonTo.get("To").toString());
// }else{
// SendMessage All ("to everyone");
// }
// }
// 
// @OnError
// public void onError(Session session, Throwable error) {
// error.printStackTrace();
// }
// 
// public void sendMessageTo(String message, String To) throws IOException {
// // session.getBasicRemote().sendText(message);
// //session.getAsyncRemote().sendText(message);
// for (WebSocketTask item : clients.values()) {
// if (item.username.equals(To) )
// item.session.getAsyncRemote().sendText(message);
// }
// }
// 
// public void sendMessageAll(String message) throws IOException {
// for (WebSocketTask item : clients.values()) {
// item.session.getAsyncRemote().sendText(message);
// }
// }
// 
// public static synchronized int getOnlineCount() {
// return onlineCount;
// }
// 
// public static synchronized void addOnlineCount() {
// WebSocketTask.onlineCount++;
// }
// 
// public static synchronized void subOnlineCount() {
// WebSocketTask.onlineCount--;
// }
// 
// public static synchronized Map<String, WebSocketTask> getClients() {
// return clients;
// }
// 
// }
