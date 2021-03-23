package edu.ualberta.storyteller.core.dataloader;

import com.alibaba.fastjson.JSONObject;
import edu.ualberta.storyteller.core.parameter.Parameters;
import edu.ualberta.storyteller.eventlayer.ESmethods;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.StringTokenizer;

/**
 * This class contains the data loader method
 * for different kinds of files.
 * <p>
 * @author Bang Liu <bang3@ualberta.ca>
 * @version 2017.1219
 */
public class DataLoaderFromES {

    /**
     * Configuration.
     */
	protected Parameters parameters;

    /**
     * Parametric constructor.
     * <p>
     * @param parameters Configuration.
     */
	public DataLoaderFromES(Parameters parameters) {
		this.parameters = parameters;
	}

    /**
     * Transform input file into corpus.
     * <p>
     * @param ipAddress Input file name.
     * @return A corpus.
     */
    public Corpus loadCorpus(String ipAddress, ArrayList<Integer> idlist) throws Exception  {
        Corpus corpus = new Corpus();//新建语料实例
        switch (parameters.dataType) {
            case "ChineseNews":
                corpus = loadChineseNews(ipAddress,//加载中文新闻
                        idlist,
                        parameters.stopwords,
                        parameters.boostRateMainKeyword,
                        parameters.boostRateNormalKeyword,
                        parameters.boostRateNormalWord);
                break;

            default://语言不匹配
                System.out.println("Unsupported input data type!");
                System.exit(1);//异常，强退程序
        }
        return corpus;
    }


    /**
     * Load Chinese news data set.
     * <p>
     * @param address File name.
     * @param stopwords Set of stop words.
     * @param boostRateMainKeyword Weight to boost up main keywords. It is usually >= 1.
     * @param boostRateNormalKeyword Weight to boost normal keywords.
     * @param boostRateNormalWord Weight to boost normal words.
     */
    public Corpus loadChineseNews(String address,
                                  ArrayList<Integer> idlist,
                                  HashSet<String> stopwords,
                                  double boostRateMainKeyword,
                                  double boostRateNormalKeyword,
                                  double boostRateNormalWord) throws Exception {  // TODO: delete parameters
                Corpus corpus = new Corpus();
                HashSet<String> corpusDocTitles = new HashSet<>();
                ESmethods.init();
                try{
                    for(int count:idlist){//最大8590
                        JSONObject jsonsource = new JSONObject();
                        jsonsource = ESmethods.SearchDocument(String.valueOf(count));

                        String url = jsonsource.getString("url");
                        String from = jsonsource.getString("from");
                        String hour_ip = jsonsource.getString("hour_ip");
                        String time = jsonsource.getString("time");
                        String first_topic = jsonsource.getString("1st_topic");
                        String[] main_keywords = jsonsource.getString("main_keywords").split(",");;
                        String[] all_keywords = jsonsource.getString("all_keywords").split(",");;
                        String segment_title = jsonsource.getString("segment_title");
                        String id = segment_title.replaceAll("\\s+","");//id其实没用
                        String segment_content = jsonsource.getString("segment_content");
                        count++;
                        // create document
                        Document d = new Document(id);//以本行标题新建一个文本实例
                        d.segTitle = segment_title;
                        d.title = segment_title.replaceAll("\\s+","");
                        if (corpusDocTitles.contains(d.title)) {
                            corpus.docs.get(id).urls.add(url);
                            corpus.docs.get(id).transformedUrls.add(url);
                        }
                        else {
                            corpusDocTitles.add(d.title);//添加标题到list里
                        }
                        d.segContent = segment_content;
                        d.topic = first_topic;
                        d.publishTime = new Timestamp((long) Double.parseDouble(time) * 1000);
                        d.language = parameters.language;

                        String[] kws = d.segTitle.split("\\s+");
                        d.titleKeywords = new HashSet<>(Arrays.asList(kws));
                        d.titleKeywords.removeAll(parameters.stopwords);

                        d.mainKeywords = new HashSet<>(Arrays.asList(main_keywords));
        //                d.allKeywords = new HashSet<>(Arrays.asList(allKeywords));
                        d.urls.add(url);
                        d.transformedUrls.add(url);
                        d.from = from;
                        // TODO: is this ok?
                        d.id = d.id;

                        // create document's keywords
                        String[] words = segment_content.split("\\s+");//使用内容制作候选词列表
                        for (int j = 0; j < words.length; ++j) {
                            // handle different words
                            double tf = 0;
                            if (Arrays.asList(main_keywords).contains(words[j])) {//该词是mainKeywords，给高得分
                                tf = 1 * boostRateMainKeyword;
                            } else if (Arrays.asList(all_keywords).contains(words[j])) {
                                tf = 1 * boostRateNormalKeyword;
                            } else {
                                tf = 1 * boostRateNormalWord;//未命中关键词，得0分
                            }
                            //只有标题词和allKeywords的交集才有资格成为真正的keywords
                            if (tf > 0 && words[j].length() > 0 && !stopwords.contains(words[j])) {//是关键词+非停止词
                                if (!d.keywords.containsKey(words[j])) {//这是新关键词
                                    d.keywords.put(words[j], new Keyword(words[j], words[j], tf, 1));//创建tf得分，df得分为1
                                } else {//是老关键词
                                    d.keywords.get(words[j]).tf += tf;//修改tf得分
                                }
                            }
                        }
                        // add new document to docs
                        corpus.docs.put(id, d);//更新语料库

                            }

                }



             catch (Exception e) {
                e.printStackTrace();
            }
        System.out.println(corpus.docs.size() + " documents are loaded.");

        // filter documents with not enough keywords
        corpus.filterDocsByNumKeywords(parameters.minDocKeywordSize);//关键词太少的文本丢弃，我们的关键词算法是外接的，没有这个问题

        // count each keyword' df and save into the hash map DF
        corpus.updateDF();//重新计算df值

        // print load information
        System.out.println(corpus.docs.size()
                + " documents remained after filtering small documents (Documents that have less than "
                + parameters.minDocKeywordSize
                + " keywords).");

        return corpus;
    }

    /**
     * Load English news data set.
     * <p>
     * @param inputFileName File name.
     * @param stopwords Set of stop words.
     * @param boostRateMainKeyword Weight to boost up main keywords. It is usually >= 1.
     * @param boostRateNormalKeyword Weight to boost normal keywords.
     * @param boostRateNormalWord Weight to boost normal words.
     * @throws Exception
     */


    /**
     * Get base form of an English word.
     * <p>
     * @param stopwords The set of stop words.
     * @param porter Used to get base form of English word.
     * @param word The word we are processing.
     * @return The base form of word.
     */
	public static String getBaseForm(HashSet<String> stopwords, Porter porter, String word) {
		String base = "";
		StringTokenizer stt = new StringTokenizer(word, "!' -_@0123456789.");
		while (stt.hasMoreTokens()) {
			String token = stt.nextToken().toLowerCase();
			if ((token.indexOf("?") == -1 && token.length() > 2 && !stopwords.contains(token)))
				base += porter.stripAffixes(token) + " ";
		}
		return base.trim();
	}

}
