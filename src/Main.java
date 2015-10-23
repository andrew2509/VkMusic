import com.google.gson.*;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;


/**
 * Created by qjex on 10/6/15.
 */
public class Main {

    public static Path path;
    public static String getToken = "https://oauth.vk.com/authorize?client_id=5096323&scope=audio&display=page&v=5.37&response_type=token";
    public static String getApiCode = "https://oauth.vk.com/authorize?client_id=5096323&scope=audio&display=page&v=5.37&response_type=code&redirect_uri=http://127.0.0.1:7856/";
    public static String getApiToken = "https://oauth.vk.com/access_token?client_id=5096323&client_secret=I8ErT0wpoMX9RHxecJ3I&redirect_uri=http://127.0.0.1:7856/&code=";
    public static boolean mobileNames = false;
    public static int albumId = -1;
    public static int ownerId = -1;

    public static void main(String[] args) {
        String key = null;
        String dir = ".";
        for(int i = 0; i < args.length; i++){
            if (args[i].equals("-d")){
                dir = args[i + 1];
                i++;
            }
            if (args[i].equals("-k")){
                key = args[i + 1];
                i++;
            }
            if (args[i].equals("-m")){
                mobileNames = true;
            }
            if (args[i].equals("-h")){
                System.out.println("-d <dir> \n-k <api key> \n-m Enable mobile filenames\n-a <album_id>\n-o <owner_id>\n");
                System.exit(0);
            }
            if (args[i].equals("-a")){
                albumId = Integer.parseInt(args[i + 1]);
                i++;
            }
            if (args[i].equals("-o")){
                ownerId = Integer.parseInt(args[i + 1]);
                i++;
            }
        }
        path = Paths.get(dir);

        if (key == null){
            key = getAccessKey();
        }
        String response = getResponse(key);
        ArrayList<Song> songList = getList(response);
        System.out.println("Got " + songList.size() + " songs");
        for(Song song : songList){
            downloadSong(song);
        }
    }

    public static void downloadSong(Song song){
        try {
            String filename = song.artist + " - " + song.title + ".mp3";
            if (mobileNames) filename = song.owner_id + "_" + song.aid;
            File file = new File(path.toFile(), filename);
            System.out.println("Downloading " + "\"" + song.artist + " - " + song.title+ "\" " + "id: " + song.aid);


            URL url = new URL(song.url.replaceAll("\\\\", ""));
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            float fileSize = Float.parseFloat(con.getHeaderField("Content-Length"));
            if (file.exists() && file.length() == fileSize) {
                System.out.println("Already downloaded");
                return;
            }
            BufferedInputStream in = new BufferedInputStream(con.getInputStream());
            FileOutputStream fout = new FileOutputStream(file);

            int BUFF_SZ = 1024;
            byte buff[] = new byte[BUFF_SZ];
            int data = 0;
            long totalDownloaded = 0;
            long downloadStartTime = System.currentTimeMillis();
            while ((data = in.read(buff, 0, BUFF_SZ)) != -1){
                totalDownloaded += data;
                long cur = System.currentTimeMillis() - downloadStartTime;
                float speed = 1000f * totalDownloaded / cur;
                printProgress(Math.round(100f * totalDownloaded / fileSize), speed);
                fout.write(buff, 0, data);
            }
            System.out.print('\n');
            if (file.length() != fileSize){
                System.out.println("\rError downloading " + song.aid);
                return;
            }
        } catch (MalformedURLException e) {
            System.err.println("Error downloading " + song.aid);
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void printProgress(int pos, float speed){
        int size = 20;
        int current = pos * size / 100;
        StringBuilder bar = new StringBuilder();
        bar.append("[");
        for(int i = 0; i < current; i++)
            bar.append("#");
        for(int i = current; i < size; i++)
            bar.append(" ");
        bar.append("]");
        System.out.print("\r" + bar + " " + getSpeed(speed));
    }

    public static String getSpeed(float speed) {
        String ut = "B/s";
        if (speed > (1024 * 1024)){
            speed /= (1024 * 1024);
            ut = "MB/s";
        }
        if (speed > 1024){
            speed /= 1024;
            ut = "KB/s";
        }
        String res = String.format("%.2f", speed);
        return (res + " " + ut);
    }

    private static ArrayList<Song> getList(String response) {
        ArrayList<Song> list = new ArrayList<>();
        System.out.println("Parsing song list");
        try {
            JsonElement jse = new JsonParser().parse(response);
            JsonObject resp = jse.getAsJsonObject();
            if (resp.has("error")){
                System.err.println("Wrong permissions");
                System.exit(1);
            }
            if (!resp.has("response")) throw new JsonParseException("No song data in response");
            JsonArray items = resp.getAsJsonArray("response");
            for(JsonElement item: items){
                if (item.isJsonObject()){
                    Gson gson = new GsonBuilder().create();
                    Song song = gson.fromJson(item, Song.class);
                    list.add(song);
                }
            }

        } catch (JsonParseException e){
            System.err.println("Error parsing song list");
            e.printStackTrace();
            System.exit(1);
        }
        return list;
    }

    private static String getAppCode() throws IOException {
        URL urlCode = null;
        try {
            urlCode = new URL(getApiCode);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            System.exit(1);
        }
        LightHTTP httpServer = new LightHTTP(7856);
        Thread t = new Thread(httpServer);
        t.start();
        openWebpage(urlCode);
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        httpServer.destroy();
        return httpServer.getCode();
    }

    private static String getAccessKey() {
        String accessKey = null;
        URL url = null;
        try {
            url = new URL(getToken);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            System.exit(1);
        }
        try {
            String code = getAppCode();
            URL urlToken = new URL(getApiToken + code);

            HttpURLConnection connection = (HttpURLConnection)urlToken.openConnection();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null){
                response.append(line);
            }
            in.close();
            JsonElement jse = new JsonParser().parse(response.toString());
            JsonObject resp = jse.getAsJsonObject();
            if (resp.has("error")){
                System.err.println("Wrong permissions");
                System.exit(1);
            }
            if (!resp.has("access_token")) throw new JsonParseException("No access token data in response");
            return resp.getAsJsonPrimitive("access_token").getAsString();

        }
        catch (IOException e) {
            openWebpage(url);

            e.printStackTrace();
            Scanner sc = new Scanner(System.in);
            System.out.println("Enter api key");
            String key = sc.next();
            return key;
        }
        catch (JsonParseException e){
            System.err.println("Error parsing access token");
            e.printStackTrace();
            System.exit(1);

        }



        return accessKey;

    }

    private static String getResponse(String key) {
        String request = "https://api.vk.com/method/audio.get?access_token=" + key;
        if (ownerId != -1) request+= "&owner_id=" + ownerId;
        if (ownerId != -1) request+= "&album_id=" + albumId;
        StringBuilder response = new StringBuilder();
        try {
            URL url = new URL(request);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            if (con.getResponseCode() != 200){
                System.err.println("Bad api key");
                System.exit(0);
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line;
            while ((line = in.readLine()) != null){
                response.append(line);
            }
            in.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (IOException e) {
            System.err.println("Error downloading music list");
            e.printStackTrace();
            System.exit(1);
        }
        return response.toString();

    }

    public static void openWebpage(URL url) {
        URI uri;
        try {
            uri = url.toURI();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Get api key: " + uri.toString());
        }
    }

}
