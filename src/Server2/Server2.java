package Server2;

import Server.Server;

import java.io.IOException;

/**
 * Created by peng on 2016/6/18.
 */
public class Server2 {
    public static void main(String[] args){
        try {
            //第二个服务器，对“POST”、“/”响应
            Server serv = new Server(81);
            serv.setRouter("/", "index2.html", "POST");
            serv.listen();
        }
        catch (IOException e){
            System.out.println(e.getMessage());
        }
    }
}
