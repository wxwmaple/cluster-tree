package edu.ualberta.storyteller.core.dataloader;

import edu.ualberta.storyteller.core.parameter.Parameters;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.sql.Timestamp;

/**
 * This class contains the data loader method
 * for different kinds of files.
 * <p>
 * @author Bang Liu <bang3@ualberta.ca>
 * @version 2017.1219
 */
public class DataLoader {

    /**
     * Configuration.
     */
	protected Parameters parameters;

    /**
     * Parametric constructor.
     * <p>
     * @param parameters Configuration.
     */
	public DataLoader(Parameters parameters) {
		this.parameters = parameters;
	}

    /**
     * Transform input file into corpus.
     * <p>
     * @param inputFileName Input file name.
     * @return A corpus.
     */
    public Corpus loadCorpus(String inputFileName) throws Exception  {
        Corpus corpus = new Corpus();
        switch (parameters.dataType) {
            case "ChineseNews":
                corpus = loadChineseNews(inputFileName,
                        parameters.stopwords,
                        parameters.boostRateMainKeyword,
                        parameters.boostRateNormalKeyword,
                        parameters.boostRateNormalWord);
                break;
            case "EnglishNews":
                corpus = loadEnglishNews(inputFileName,
                        parameters.stopwords,
                        parameters.boostRateMainKeyword,
                        parameters.boostRateNormalKeyword,
                        parameters.boostRateNormalWord);
                break;
            default:
                System.out.println("Unsupported input data type!");
                System.exit(1);
        }
        return corpus;
    }


    /**
     * Load Chinese news data set.
     * <p>
     * @param inputFileName File name.
     * @param stopwords Set of stop words.
     * @param boostRateMainKeyword Weight to boost up main keywords. It is usually >= 1.
     * @param boostRateNormalKeyword Weight to boost normal keywords.
     * @param boostRateNormalWord Weight to boost normal words.
     */
    public Corpus loadChineseNews(String inputFileName,
                                  HashSet<String> stopwords,
                                  double boostRateMainKeyword,
                                  double boostRateNormalKeyword,
                                  double boostRateNormalWord) throws Exception {  // TODO: delete parameters
        //打开文件流
        File inputFile = new File(inputFileName);
        BufferedReader in = new BufferedReader(new FileReader(inputFile));
        //实例化文本列表+文本标题集合
        Corpus corpus = new Corpus();
        HashSet<String> corpusDocTitles = new HashSet<>();

        //准备变量接数据
        System.out.println("Start loading: " + inputFileName);//开始读取训练集文件
        String header = in.readLine();//第一行
        String[] cols = header.split("\\|");//用符号"|"分割标题栏
        int numCols = cols.length;
        int idxOfSegTitle = Arrays.asList(cols).indexOf("segment_title");
        int idxOfSegContent = Arrays.asList(cols).indexOf("segment_content");
        int idxOfTopic = Arrays.asList(cols).indexOf("1st_topic");
        int idxOfTimestamp = Arrays.asList(cols).indexOf("time");
        int idxOfAllKeywords = Arrays.asList(cols).indexOf("all_keywords");
        int idxOfMainKeywords = Arrays.asList(cols).indexOf("main_keywords");
        int idxOfUrl = Arrays.asList(cols).indexOf("url");
        int idxOfFrom = Arrays.asList(cols).indexOf("from");

        //读数据
        String line;
        int i = 0;
        while ((line = in.readLine()) != null) {//读剩下的内容
            try {
                // get document information from parsed line
                String[] tokens = line.split("\\|");

                // skip bad rows that contains some empty column value.
                if (tokens.length != numCols) {
                    continue;//缺信息的行不要
                }

                // read different parts of a document from a line
                // NOTICE: We use title as id to filter same documents.
                String id = tokens[idxOfSegTitle].replaceAll("\\s+","");//用完整标题作为id，说明uid其实没用
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

                if (timestamp.equals("time")) {
                    continue;  // TODO: notice, here we are filtering header like lines
                }

                // check whether already exist
                if (corpus.docs.containsKey(id)) {//如果新进文章的标题重了
                    corpus.docs.get(id).urls.add(url);
                    corpus.docs.get(id).transformedUrls.add(url);
                    continue;
                }

                // create documen
                Document d = new Document(id);//以本行标题新建一个文本实例
                d.segTitle = segTitle;
                d.title = segTitle.replaceAll("\\s+","");
                if (corpusDocTitles.contains(d.title)) {
                    corpus.docs.get(id).urls.add(url);
                    corpus.docs.get(id).transformedUrls.add(url);
                    continue;
                }
                else {
                    corpusDocTitles.add(d.title);//添加标题到list里
                }
                d.segContent = segContent;
                d.topic = topic;
                d.publishTime = new Timestamp((long) Double.parseDouble(timestamp) * 1000);
                d.language = parameters.language;

                String[] kws = d.segTitle.split("\\s+");
                d.titleKeywords = new HashSet<>(Arrays.asList(kws));
                d.titleKeywords.removeAll(parameters.stopwords);

                d.mainKeywords = new HashSet<>(Arrays.asList(mainKeywords));
                d.urls.add(url);
                d.transformedUrls.add(url);
                d.from = from;
                d.id = d.title;

                //确定文章的关键词
                String[] words = segContent.split("\\s+");//使用内容制作候选词列表
                for (int j = 0; j < words.length; ++j) {
                    // handle different words
                    double tf = 0;
                    if (Arrays.asList(mainKeywords).contains(words[j])) {//该词是mainKeywords，给高得分
                        tf = 1 * boostRateMainKeyword;
                    } else if (Arrays.asList(allKeywords).contains(words[j])) {
                        tf = 1 * boostRateNormalKeyword;
                    } else {
                        tf = 1 * boostRateNormalWord;//未命中关键词，得0分
                    }
                    //只有内容和allKeywords的交集才有资格成为真正的keywords
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

                if (++i % 10000 == 0) {
                    System.out.println(i + " documents are loaded.");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        in.close();
        System.out.println(corpus.docs.size() + " documents are loaded.");

        //关键词太少的文本丢弃，我们的关键词算法是外接的，没有这个问题
        corpus.filterDocsByNumKeywords(parameters.minDocKeywordSize);

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
    public Corpus loadEnglishNews(String inputFileName,
                                  HashSet<String> stopwords,
                                  double boostRateMainKeyword,
                                  double boostRateNormalKeyword,
                                  double boostRateNormalWord) throws Exception {  // TODO: delete parameters
        // open input file, each line is a news document
        File inputFile = new File(inputFileName);
        BufferedReader in = new BufferedReader(new FileReader(inputFile));
        Corpus corpus = new Corpus();

        // read header line and get index of different columns
        System.out.println("Start loading: " + inputFileName);
        String header = in.readLine();
        String[] cols = header.split("\\|");
        int numCols = cols.length;
        int idxOfId = Arrays.asList(cols).indexOf("id");
        int idxOfContent = Arrays.asList(cols).indexOf("original");
        int idxOfKeywords = Arrays.asList(cols).indexOf("keywords");
        int idxOfLda = Arrays.asList(cols).indexOf("LDA");

        // read each line to create documents
        String line;
        int i = 0;
        while ((line = in.readLine()) != null) {
            try {
                // get document information from parsed line
                String[] tokens = line.split("\\|");

                // skip bad rows that contains some empty column value.
                if (tokens.length != numCols) {
                    continue;
                }

                // read different parts of a document from a line
                String id = tokens[idxOfId];
                String content = tokens[idxOfContent];
                String[] keywords = tokens[idxOfKeywords].split(",");
                String lda = tokens[idxOfLda];

                // check whether already exist
                if (corpus.docs.containsKey(id)) {
                    continue;
                }

                // create document
                Document d = new Document(id);
                d.segTitle = "";
                d.title = "";
                d.segContent = content;
                d.lda = lda;
                d.language = "English";

                // create document's keywords
                String[] words = content.split("\\s+");
                for (int j = 0; j < words.length; ++j) {
                    // handle different words
                    double tf = 0;
                    if (Arrays.asList(keywords).contains(words[j])) {
                        tf = 1 * boostRateNormalKeyword;
                    } else {
                        tf = 1 * boostRateNormalWord;
                    }

                    // add the word token as document's keyword or update existing keyword's tf
                    if (tf > 0 && words[j].length() > 0 && !stopwords.contains(words[j])) {
                        if (!d.keywords.containsKey(words[j])) {
                            d.keywords.put(words[j], new Keyword(words[j], words[j], tf, 1));
                        } else {
                            d.keywords.get(words[j]).tf += tf;
                        }
                    }
                }

                // add new document to docs
                corpus.docs.put(id, d);

                if (++i % 10000 == 0) {
                    System.out.println(i + " documents are loaded.");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        in.close();
        System.out.println(corpus.docs.size() + " documents are loaded.");

        // filter documents with not enough keywords
        corpus.filterDocsByNumKeywords(parameters.minDocKeywordSize);

        // count each keyword' df and save into the hash map DF
        corpus.updateDF();

        // print load information
        System.out.println(corpus.docs.size()
                + " documents remained after filtering small documents (Documents that have less than "
                + parameters.minDocKeywordSize
                + " keywords).");

        return corpus;
    }

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
