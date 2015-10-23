import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

/**
 * Created by qjex on 10/22/15.
 */
public class LightHTTP implements Runnable{
    private ServerSocket server;
    private  boolean running = true;

    String code = null;

    LightHTTP(int port) throws IOException {
        server = new ServerSocket(port);
    }


    @Override
    public void run() {
        while (running){
            try {
                Socket socket = server.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream());

                StringBuilder request = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.length() == 0) break;
                    request.append(line);
                }
                out.print("HTTP/1.1 200 OK\r\n");
                out.print("Content-Type: text/plain\r\n");
                out.print("Connection: close\r\n");
                out.print("\r\n");
                in.close();
                out.close();
                socket.close();

                StringTokenizer st = new StringTokenizer(request.toString());
                if (!st.nextToken().equals("GET")) continue;

                String req = st.nextToken();
                if (req.startsWith("/?code")){
                    code = parseCode(req);
                    break;
                }

            } catch (IOException e) {
                break;
            }
        }
    }

    private String parseCode(String req) {
        int start = 0;
        for(int i = 0; i < req.length(); i++)
            if (req.charAt(i) == '='){
                start = i + 1;
                break;
            }
        int end = req.length();
        for(int i = start; i < req.length(); i++)
            if (req.charAt(i) == '&'){
                end = i - 1;
                break;
            }

        return req.substring(start, end);


    }



    public String getCode() {
        return code;
    }

    public void destroy(){
        running = false;
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}

