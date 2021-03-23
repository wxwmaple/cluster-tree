package edu.ualberta.storyteller.eventlayer;

import edu.ualberta.storyteller.core.parameter.*;
import edu.ualberta.storyteller.core.dataloader.*;
import edu.ualberta.storyteller.core.eventdetector.*;
import edu.ualberta.storyteller.core.util.TimeUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * This class implements the procedures for online system.
 */
public class EventLayer {

    /**
     * Show sample usage of event layer.
     * @param args Program arguments.
     * @throws Exception
     */

    public static void main(String args[]) throws Exception{
        // load params
        ArrayList<Integer> ids = new ArrayList<>();
        for(int i=1;i<=8590;i++){
            ids.add(i);
        }//创建一个id_list，这是服务器上现有的全量数据
        Cluster(ids);//聚类主函数
    }
    static void Cluster(ArrayList<Integer> idlist) throws Exception {
        String fNews = "../test_data/trump_selected.txt";//测试集，其实是假参数，只有ids才有用了
        String fParameters = "conf/ChineseNewsParameters.txt";//参数文件
        String fOutputEvents = "../test_data/events0322.txt";//输出写在此文件

        // initialization
        Parameters parameters = new Parameters(fParameters);//自定义的一个参数类实例
        DataLoader loader = new DataLoader(parameters);//加载参数到loader实例

        // load corpus
        Corpus corpus = loader.loadCorpus(fNews);//根据loader里的参数，加载相应语言的语料，并做一定的过滤

        System.out.println("Corpus size is " + corpus.docs.size() + " after filter by topics.");

        // extract events
        EventDetector eventDetector = new EventDetector(parameters);//社区探测
        ArrayList<Event> events = eventDetector.extractEventsFromCorpus(corpus);//事件探测


        // output new events
        File outputEventFile = new File(fOutputEvents);
        String eventFileFolder = outputEventFile.getAbsoluteFile().getParent();
        String eventFilePath = eventFileFolder + File.separator + fOutputEvents;
        PrintStream outputEventStream = new PrintStream(eventFilePath);
        EventDetector.printTopics(events, outputEventStream);
        outputEventStream.close();
    }
}
