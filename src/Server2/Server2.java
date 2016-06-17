package Server2;

import Server.Server;

import java.io.IOException;

/**
 * Created by peng on 2016/6/18.
 */
public class Server2 {
    public static void main(String[] args){
        try {
            Server serv = new Server(81);
            serv.setRouter("/redirect", "index2.html");
            serv.listen();
        }
        catch (IOException e){
            System.out.println(e.getMessage());
        }
    }
}
