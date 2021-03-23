package edu.ualberta.storyteller.eventlayer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.common.settings.Settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class ESmethods {
    //创建JSONObject对象
    static List<JSONObject> list= new ArrayList<>();
    public static void main(String[] args) throws Exception{
//        dataloader("D:/Desktop/StoryForest/test_data/trump0308.txt");
        init();
        SearchDocument("100");
//        CreateIndex("test_index");
//        search();
//        CreateDocument();
    }
    static void dataloader(String inputFileName) throws Exception {  // TODO: delete parameters
        // open input file, each line is a Chinese news document
        File inputFile = new File(inputFileName);
        BufferedReader in = new BufferedReader(new FileReader(inputFile));
        HashSet<String> corpusDocTitles = new HashSet<>();
        int count = 0;
        // read header line and get index of different columns
        System.out.println("Start loading: " + inputFileName);//开始读取训练集文件
        String header = in.readLine();//第一行
        String[] cols = header.split("\\|");//用符号"|"分割标题栏
        int numCols = cols.length;
        int idxOfId = Arrays.asList(cols).indexOf("id");//求每个属性的id，这里把Array转成了List
        int idxOfSegTitle = Arrays.asList(cols).indexOf("segment_title");
        int idxOfSegContent = Arrays.asList(cols).indexOf("segment_content");
        int idxOfTopic = Arrays.asList(cols).indexOf("1st_topic");
        int idxOfHourIp = Arrays.asList(cols).indexOf("hour_ip");
        int idxOfTimestamp = Arrays.asList(cols).indexOf("time");
        int idxOfAllKeywords = Arrays.asList(cols).indexOf("all_keywords");
        int idxOfMainKeywords = Arrays.asList(cols).indexOf("main_keywords");
        int idxOfUrl = Arrays.asList(cols).indexOf("url");
        int idxOfFrom = Arrays.asList(cols).indexOf("from");
        int idxOfDocId = Arrays.asList(cols).indexOf("id");  // TODO: I forgot why need this
        System.out.println(numCols);
        // read each line to create documents
        String line = in.readLine();//第二行
        while (line != null) {//读剩下的内容
            try {
                // get document information from parsed line
                String[] tokens = line.split("\\|");
                JSONObject json = new JSONObject();
                json.put("id", tokens[idxOfId]);
                json.put("url", tokens[idxOfUrl]);
                json.put("from", tokens[idxOfFrom]);
                json.put("hour_ip", tokens[idxOfHourIp]);
                json.put("time", tokens[idxOfTimestamp]);
                json.put("1st_topic", tokens[idxOfTopic]);
                json.put("main_keywords", tokens[idxOfMainKeywords]);
                json.put("all_keywords", tokens[idxOfAllKeywords]);
                json.put("segment_title", tokens[idxOfSegTitle]);
                json.put("segment_content", tokens[idxOfSegContent]);
                list.add(json);
                line = in.readLine();//取完赶紧更新

                if (tokens.length != numCols) {
                    continue;//缺信息的行不要
                }
                // read different parts of a document from a line
                // NOTICE: We use title as id to filter same documents.
                String id = tokens[idxOfSegTitle].replaceAll("\\s+","");//用完整标题作为id，说明uid其实没用
                String docId = tokens[idxOfDocId];//没用
                String segTitle = tokens[idxOfSegTitle];
                String segContent = tokens[idxOfSegContent];
                String topic = tokens[idxOfTopic];
                String timestamp = tokens[idxOfTimestamp];
                String[] allKeywords = tokens[idxOfAllKeywords].split(",");
                String[] mainKeywords = tokens[idxOfMainKeywords].split(",");

                String url;
                String from;
                if (idxOfUrl != -1) {
                    url = tokens[idxOfUrl];//虽然上文过滤过一次，但仍可能不含url和from
                } else {
                    url = "www.fake-url.com";
                }
                if (idxOfFrom != -1) {
                    from = tokens[idxOfFrom];
                } else {
                    from = "fake-from";
                }



            } catch (Exception e) {
                continue;
            }
        }
        in.close();
    }
    static RestClient restClient;

    public static void init() throws UnknownHostException {
        //指定ES集群
        Settings settings = Settings.builder().put("cluster.name", "elasticsearch").build();

        //创建访问es服务器的客户端
        restClient = RestClient.builder(
                new HttpHost("172.20.46.166", 9200, "http")).build();

    }

    public static void CreateIndex(String IndexName) throws IOException {
        //数据查询
        Request request = new Request(
                "PUT",
                "/"+IndexName);
        request.addParameter("pretty", "true");
        Response response = restClient.performRequest(request);

        //得到查询到的数据
        System.out.println(EntityUtils.toString(response.getEntity()));

//        restClient.close();

    }
    //从es中查询数据
    public static JSONObject SearchDocument(String DocumentId) throws IOException {
        //数据查询
        Request request = new Request(
                "GET",
                "/test_index/type1/"+DocumentId);
        request.addParameter("pretty", "true");
        Response response = restClient.performRequest(request);

        String str = EntityUtils.toString(response.getEntity());
//        JSONObject json_test = JSONObject.parse(str);
        JSONObject jsonObject = JSON.parseObject(str);
        String source = jsonObject.getString("_source");
        JSONObject jsonsource = JSON.parseObject(source);
        return jsonsource;



//        restClient.close();

    }
    static void CreateDocument()throws Exception{
        int count = 1;
        for(JSONObject item:list){
            Request request = new Request("PUT", "/test_index/type1/"+String.valueOf(count));
            String strJson=item.toString();
            request.setEntity(new NStringEntity(
                    strJson,
                    ContentType.APPLICATION_JSON));
            Response response = restClient.performRequest(request);
            count++;
            if(count%100==0){
                System.out.println(count+strJson);
            }
        }

//        System.out.println(response.getEntity());
    }
}
