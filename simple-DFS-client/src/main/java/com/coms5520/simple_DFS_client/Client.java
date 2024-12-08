package com.coms5520.simple_DFS_client;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class Client {
    private static String host = "http://localhost:8080";

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        FileHandler fh = new FileHandler();
        int responseCode;
        HttpURLConnection conn;

        System.out.println("Loading the file store in the Simple DFS...");
        conn = (HttpURLConnection) new URL(host+"/api/metadata/getList").openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        responseCode = conn.getResponseCode();
        if(responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = br.readLine()) != null) {
                content.append(inputLine.trim());
            }
            br.close();

            JSONObject response = new JSONObject(content.toString());
            String line = response.getString("list");
            String[] lines = line.split(",");
            for(String s : lines) {
                System.out.println(s);
            }
        }
        conn.disconnect();
        System.out.println("\n");
        Scanner scanner = new Scanner(System.in);
        while(true) {
            System.out.println("Please Enter your command:\nLIST for get file List in the SDFS\n" +
                    "UPLOAD {filename} for upload new file into SDFS\n" +
                    "DOWNLOAD {filename} {true or false} for download file into your disk\n" +
                    "\"the third argument indicate if you want retrieve replica version\"\n" +
                    "DELETE {filename} for remove file from SDFS\n"
            );
            String input = scanner.nextLine();
            if(input.startsWith("UPLOAD")) {
                String[] arg = input.split(" ");
                File file = new File(arg[1]);
                fh.uploadFile(file,host + "/api/metadata");
                continue;
            }
            if(input.startsWith("DOWNLOAD")) {
                String[] arg = input.split(" ");
                if(arg[2].equals("true")) {
                    conn = (HttpURLConnection) new URL(host+"/api/metadata/downloadr/" + arg[1]).openConnection();
                }else{
                    conn = (HttpURLConnection) new URL(host+"/api/metadata/download/" + arg[1]).openConnection();
                }
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                responseCode = conn.getResponseCode();
                if(responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = br.readLine()) != null) {
                        content.append(inputLine.trim());
                    }
                    br.close();
                    JSONObject response = new JSONObject(content.toString());
                    String line = response.getString("nodes");
                    fh.retrieveFile(arg[1],line,true);
                }else{
                    System.out.println("File not found");
                }
                conn.disconnect();
                continue;
            }
            if(input.startsWith("DELETE")) {
                String[] arg = input.split(" ");
                conn = (HttpURLConnection) new URL(host+"/api/metadata/" + arg[1]).openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("Accept", "application/json");

                responseCode = conn.getResponseCode();
                if(responseCode == HttpURLConnection.HTTP_OK) {
                    System.out.println("File deleted successfully");
                }else{
                    System.out.println("File not found" + responseCode);
                }
                conn.disconnect();
                continue;
            }
            if(input.startsWith("LIST")) {
                conn = (HttpURLConnection) new URL(host+"/api/metadata/getList").openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                responseCode = conn.getResponseCode();
                if(responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = br.readLine()) != null) {
                        content.append(inputLine.trim());
                    }
                    br.close();

                    JSONObject response = new JSONObject(content.toString());
                    String line = response.getString("list");
                    String[] lines = line.split(",");
                    for(String s : lines) {
                        System.out.println(s);
                    }
                }
                conn.disconnect();
                System.out.println("\n");
                continue;
            }
        }
    }
}
