package Server;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.net.Socket;
import java.net.ServerSocket;

import Crypt.Crypt;

class Define{
    final static public int[] status_code={200, 304, 400, 404, 500};
    final static public String[] status={"OK", "Not Modified", "Bad Request",  "Not Found", "Server Error"};
    final static public String[] method={"GET", "POST"};
}
/**
 * Created by peng on 2016/6/16.
 */
public class Server extends ServerSocket{
    private int port;
    public Server()throws IOException{
        super(80);
        port=80;
    }
    public Server(int port)throws IOException {
        super(port);
        this.port=port;
    }

    //启动HTTP服务
    public void listen()throws IOException{
        System.out.println("Server start");
        while(true){
            Socket socket=accept();
            new Thread(new Handle(socket)).start();
        }
    }

    //添加到指定文件的路由方法
    public void setRouter(String path, String file, String method){
        if(method.equals(Define.method[0]))Handle.get_router.put(path, file);
        else if(method.equals(Define.method[1]))Handle.post_router.put(path, file);
    }

    //添加指定路径的重定向，重定向到81端口
    public void setRedirect(String path, String method){

        if(method.equals(Define.method[0]))Handle.get_redirect.add(path);
        else if(method.equals(Define.method[1]))Handle.post_redirect.add(path);
    }

    public static void main(String[] args){
        try {

            //第一个服务器，对“get”、“/”响应，对“POST”、“/”重定向
            Server serv = new Server();
            serv.setRouter("/", "index.html", "GET");
            serv.setRedirect("/", "POST");
            serv.listen();
        }
        catch (IOException e){
            System.out.println(e.getMessage());
        }
    }
}

class Handle implements Runnable{
    private Socket socket;
    private BufferedReader br;
    private String req_source;
    private BufferedWriter bw;
    private HashMap<String, String> reqHeaders;
    private HashMap<String, String> resHeaders;
    static HashMap<String, String> get_router=new HashMap<>();
    static HashMap<String, String> post_router=new HashMap<>();
    static HashSet<String> get_redirect = new HashSet<String>();
    static HashSet<String> post_redirect = new HashSet<String>();
    private int response_code;
    private String response;
    private String version;
    Handle(Socket socket){
        this.socket=socket;
        resHeaders=new HashMap<>();
        reqHeaders=new HashMap<>();
        resHeaders.put("Connection", "close");
    }

    //生成HTTP响应信息，响应主体为字符串
    private StringBuffer send(String str)throws IOException{
        StringBuffer buf=new StringBuffer();
        buf.append(version);
        buf.append(" ");
        buf.append(new Integer(response_code).toString());
        buf.append(" ");
        buf.append(response);
        buf.append("\r\n");
        for (String key:resHeaders.keySet()
             ) {
            buf.append(key);
            buf.append(":");
            buf.append(resHeaders.get(key));
            buf.append("\r\n");
        }
        buf.append("\r\n");
        buf.append(str);
        //bw.flush();
        return buf;
    }

    //生成HTTP响应，响应主体为某个文件
    private StringBuffer sendFile(String filename)throws IOException{
        try{
            BufferedReader fi=new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
            BufferedReader fir=new BufferedReader(fi);
            StringBuffer buf=new StringBuffer();
            ByteBuffer byteBuffer;
            buf.append(version);
            buf.append(" ");
            buf.append(Integer.toString(response_code));
            buf.append(" ");
            buf.append(response);
            buf.append("\r\n");;
            for (String key:resHeaders.keySet()
                    ) {
                buf.append(key);
                buf.append(":");
                buf.append(resHeaders.get(key));
                buf.append("\r\n");
            }
            buf.append("\r\n");
            String temp;
            while ((temp=fi.readLine())!=null){
                buf.append(temp);
            }
            buf.append("\r\n");
            return buf;
        }
        catch (FileNotFoundException e){
            System.out.println(e.getMessage());
            response_code=404;
            response=Define.status[3];
            return send("File Not Found");
        }
    }


    @Override
    public void run(){
        try {
            br =new BufferedReader(new InputStreamReader(socket.getInputStream()) );
            bw=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            String line=br.readLine();
            String[] ss=line.split(" ");
            if(ss.length<3){
                response_code=400;
                response=Define.status[2];
                send("Bad Request");
            }
            else{
                req_source=ss[1];
                version=ss[2];
                System.out.print(ss[0]+" "+ss[1]);

                //路由命中时
                if((get_router.keySet().contains(req_source)&&ss[0].equals(Define.method[0])) || (post_router.keySet().contains(req_source)&&ss[0].equals(Define.method[1]))){

                    response_code = 200;
                    response = Define.status[0];
                    while ((line = br.readLine()) != null && !line.equals("") && !line.equals("\n") && !line.equals("\r\n")) {
                        ss = line.split(":");
                        if (ss.length >= 2) {
                            reqHeaders.put(ss[0], ss[1]);
                        }
                    }
                    StringBuffer ret;
                    if (get_router.keySet().contains(req_source))
                        ret=sendFile(get_router.get(req_source));
                    else
                        ret=sendFile(post_router.get(req_source));

                    //若为HTTPS，则返回结果加密
                    if(version.equals("HTTPS/1.1")){
                        Crypt cp=new Crypt();
                        cp.loadKey("KeyPair");
                        byte[] encryData=cp.encrypt(ret.toString().getBytes());
                        BufferedOutputStream bo=new BufferedOutputStream(socket.getOutputStream());
                        bo.write(encryData);
                        bo.write("\n".getBytes());
                        byte[] signature=cp.sign(ret.toString().getBytes());
                        bo.write(signature);
                        bo.write("\n".getBytes());
                        bo.flush();
                    }
                    else {
                        bw.write(ret.toString());
                        bw.flush();
                    }

                }

                //重定向命中时，发送HTTPS的请求到81端口
                else if((get_redirect.contains(req_source)&&ss[0].equals(Define.method[0])) || (post_redirect.contains(req_source)&&ss[0].equals(Define.method[1]))){
                    Socket socket2=new Socket("localhost", 81);
                    BufferedWriter bw2=new BufferedWriter(new OutputStreamWriter(socket2.getOutputStream()));
                    String buf;
                    bw2.write(ss[0]+" "+req_source+" HTTPS/1.1\r\n");
                    while((buf=br.readLine())!=null && !buf.equals("")){
                        ss=buf.split(":");
                        if(ss.length>=1&&ss[0].equals("Host")){
                            bw2.write("Host: localhost:81\r\n");
                        }
                        else{
                            bw2.write(buf);
                            bw2.write("\r\n");
                        }
                    }
                    bw2.write("\r\n");
                    bw2.flush();
                    BufferedReader br2=new BufferedReader(new InputStreamReader(socket2.getInputStream()));
                    Crypt cp=new Crypt();
                    cp.loadKey("KeyPair");
                    String encryData=br2.readLine();
                    String signature=br2.readLine();
                    byte[] result=cp.decrypt(encryData.getBytes());
                    if(!cp.verify(result, signature.getBytes())){
                        response_code=500;
                        response=Define.status[4];
                        StringBuffer sb=send("Verify Failed");
                        bw.write(sb.toString());

                    }
                    else{
                        bw.write(new String(result));
                    }
                    bw.flush();
                    br2.close();
                    bw2.close();
                    socket2.close();
                }
                else{
                    response_code=404;
                    response=Define.status[3];
                    StringBuffer ret=send("Couldn't find that");
                    bw.write(ret.toString());
                    bw.flush();
                }
            }
            System.out.println(" "+response_code);
            socket.close();
            br.close();
            bw.close();

        }
        catch(IOException e){
            System.out.println(e.getMessage());
        }

    }
}



