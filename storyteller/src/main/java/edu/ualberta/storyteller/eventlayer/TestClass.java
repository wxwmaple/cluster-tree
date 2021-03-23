package edu.ualberta.storyteller.eventlayer;

import java.io.*;
import java.sql.Timestamp;
import java.util.*;

public class TestClass {

    public static void main(String[] args) throws Exception{
        new TestClass().dataloader("../test_data/trump0308.txt");
    }
    void dataloader(String inputFileName) throws Exception {  // TODO: delete parameters
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
                System.out.println(tokens[0]);
                System.out.println(tokens[1]);
                System.out.println(tokens[2]);
                System.out.println(tokens[3]);
                System.out.println(tokens[4]);
                System.out.println(tokens[5]);
                System.out.println(tokens[6]);
                System.out.println(tokens[7]);
                System.out.println(tokens[8]);
                System.out.println(tokens[9]);
                System.out.println(tokens[10]);
                System.out.println("~~~~~");
                line = in.readLine();//取完赶紧更新

                if (tokens.length != numCols) {
                    continue;//缺信息的行不要
                }
                System.out.println("777");
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

                System.out.println(segTitle);
                count++;
                if(count==10){
                    break;
                }

            } catch (Exception e) {
                continue;
            }
        }
        in.close();
    }
}
