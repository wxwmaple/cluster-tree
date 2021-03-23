package edu.ualberta.storyteller.core.parameter;

import edu.ualberta.storyteller.core.util.NlpUtils;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

public class Parameters implements Serializable {//实现序列化接口
    // 序列化的好处：可以被ObjectOutputStream转换为字节流，同时也可以通过ObjectInputStream再将其解析为对象
    //对象->（序列化）字节流->写入文件
    //读文件->字节流->（反序列化）对象

    //语言
    public String language;

    //数据类型
    public String dataType;

    //常规词的TF boost得分
    public double boostRateNormalWord = 0;

    public double boostRateMainKeyword = 3;

    public double boostRateNormalKeyword = 1;

    //文章到关键词图的最小余弦相似度
    public double minSimDoc2KeyGraph = .25;

    //Todo:社区发现算法类型？
    public String communityDetectAlg = "Betweenness";

    //Todo:事件发现算法类型？
    //! Currently: "DocRelation", "DocGraph", "Rule"
    public String eventSplitAlg = "DocRelation";

    //话题文本数阈值，太少的不构成话题
    public int minTopicSize = 5;

    //关键词词频阈值，太少的不参与图构建
    public int minNodeDF = 4;

	//关键词共现次数阈值，太少的不参与图构建
    public int minEdgeDF = 3;

	//! Minimum edge correlation of keyword graph edges.
	//! KeywordEdge (n1, n2) will smaller correlation will be filtered
	//! (which means node n1 n2 may also be connected with a lot of other nodes).
    //! NOTICE: this param influence the final clusters a lot.
	public double minEdgeCorrelation = .15;

    //关键词词频上界，太常见的参与图构建
    public double maxNodeDFPercent = .3;

    //社区检测后，一个社区至少要3个keyword才能被视为topic
    public int minClusterNodeSize = 3;

    //一个社区的关键词过多，要继续切分
    public int maxClusterNodeSize = 800;

    //文章不能有太少的关键词
    public int minDocKeywordSize = 5;

    //! Merge two document clusters if their intersect proportion is bigger than a threshold
    public double minIntersectPercentToMergeCluster = .3;

    //! Minimum conditional probability to duplicate an edge rather than delete it from keyword graph.
    //! Duplicate the edge if the conditional probability is higher than this threshold.
    public double minCpToDuplicateEdge = 1.7;

    //! Whether use document's topic to split a document cluster into sub clusters.
    public boolean useDocumentTopic = false;

    //! Whether use document's title common word's count to split a document cluster into sub clusters.
    public boolean useDocumentTitleCommonWords = false;

    //! Minimum number of common words in title to taken two documents into one group.
    public int minTitleCommonWordsSize = 2;

    //! Minimum percentage of common words in title to taken two documents into one group.
    public double minTitleCommonWordsPercent = .4;

    //! The file that contains stop words. Each line is a word.
    public String fStopwords;
    public HashSet<String> stopwords;

    //! The file that contains SVM model to determine whether two documents are of same event.
    public String fModel;
    public libsvm.svm_model model;

    public String fSameStoryModel;
    public libsvm.svm_model sameStoryModel;

    //! Minimum key graph compatibility for matching a document cluster to an existing story tree.
    public double minKeygraphCompatibilityDc2St = .6;

    //! Minimum compatibility for matching a document cluster to an existing story tree's node.
    public double minCompatibilityDc2Sn = .3;

    //! Minimum tf cosine similarity for matching a document cluster to an existing story tree's node.
    public double minTFCosineSimilarityDc2Sn = .02;

    //! Used to control the influence of timestamp difference between new document and old document.
    //! Assume if time gap is bigger, the possibility that two documents are connected will decay by exponential func.
    //! If this param is bigger, the decay will be faster; otherwise, the decay is smoother.
    //! Calculate time proximity.
    public double deltaTimeGap = .5;

    //! Used to control the influence of document distribution.
    //! Calculate document distributional proximity.
    public double deltaDocDistribution = .5;

    //! How many day's data to keep.
    public int historyLength = 3;

    //! Whether use extra title for topic match.
    public boolean useRelatedNewsTitlesForMatch = false;

    //! The file that contains SVM model to determine whether a document matches a query.
    public String fQueryDocMatchModel;
    public libsvm.svm_model queryDocMatchModel;

    //! Maximum number of docs to match for a topic.
    public int maxMatchedDocsSize = 20;

    /**
     * Parametric constructor.
     * Create Parameters instance from file.
     * @param paramsFile File name of parameters' file.
     * @throws Exception
     */
	public Parameters(String paramsFile) throws Exception {//在构造函数中捕获异常，在调用时必须手动try/catch捕获异常
		load(new DataInputStream(new FileInputStream(paramsFile)));
		//FileInputStream：用于从文件读取数据，传入路径字符串形式的文件名即可
        //DataInputStream：从底层数据流中读取数据，如FileInputStream
	}

    /**
     * Load parameters from parameter file.
     * @param in Parameter file input stream.
     * @throws Exception
     */
	public void load(DataInputStream in) throws Exception {
		HashMap<String, String> conf = new HashMap<String, String>();//一个键值对集合，如k1=v1，用于存储参数
		String line = null;
		while ((line = in.readLine()) != null) {//循环读取line
			line = line.trim();//删除头尾空白
			if (line.startsWith("//") || line.length() == 0) {//注释行或者空行
                continue;
            }

			StringTokenizer st = new StringTokenizer(line, "= ;");//字符串分词器，按等号和分号分割
			conf.put(st.nextToken(), st.nextToken());//加入键值对
		}

        // parameters for experimental setup
        language = conf.get("language");
        dataType = conf.get("dataType");

        // parameters to boost word tf
        boostRateNormalWord = Double.parseDouble(conf.get("boostRateNormalWord"));//强制转换格式为double
        boostRateMainKeyword = Double.parseDouble(conf.get("boostRateMainKeyword"));
        boostRateNormalKeyword = Double.parseDouble(conf.get("boostRateNormalKeyword"));

        // parameters to filter documents
        minDocKeywordSize = Integer.parseInt(conf.get("minDocKeywordSize"));

        // parameters to filter keyword graph nodes
        minNodeDF = Integer.parseInt(conf.get("minNodeDF"));
		maxNodeDFPercent = Double.parseDouble(conf.get("maxNodeDFPercent"));

        // parameters to filter keyword graph edges
		minEdgeDF = Integer.parseInt(conf.get("minEdgeDF"));
        minEdgeCorrelation = Double.parseDouble(conf.get("minEdgeCorrelation"));

        // parameters to detect keyword graph communities
        communityDetectAlg = conf.get("communityDetectAlg");

        // parameters to split or filter keyword graphs
		maxClusterNodeSize = Integer.parseInt(conf.get("maxClusterNodeSize"));
		minClusterNodeSize = Integer.parseInt(conf.get("minClusterNodeSize"));
		minIntersectPercentToMergeCluster = Double.parseDouble(conf.get("minIntersectPercentToMergeCluster"));
        minCpToDuplicateEdge = Double.parseDouble(conf.get("minCpToDuplicateEdge"));

        // parameters to assign document to keyword graphs
        minSimDoc2KeyGraph = Double.parseDouble(conf.get("minSimDoc2KeyGraph"));

        // parameters to filter document clusters
        minTopicSize = Integer.parseInt(conf.get("minTopicSize"));

        // parameters to processing document clusters
        useDocumentTopic = Boolean.parseBoolean(conf.get("useDocumentTopic"));
        useDocumentTitleCommonWords = Boolean.parseBoolean(conf.get("useDocumentTitleCommonWords"));
        minTitleCommonWordsSize = Integer.parseInt(conf.get("minTitleCommonWordsSize"));
        minTitleCommonWordsPercent = Double.parseDouble(conf.get("minTitleCommonWordsPercent"));
        fStopwords = conf.get("fStopwords");
        stopwords = NlpUtils.importStopwords(fStopwords, language);//把停止词加进来
        eventSplitAlg = conf.get("eventSplitAlg");

        // parameters to merge new documents wit stories
        minKeygraphCompatibilityDc2St = Double.parseDouble(conf.get("minKeygraphCompatibilityDc2St"));
        minCompatibilityDc2Sn = Double.parseDouble(conf.get("minCompatibilityDc2Sn"));
        minTFCosineSimilarityDc2Sn = Double.parseDouble(conf.get("minTFCosineSimilarityDc2Sn"));
        deltaTimeGap = Double.parseDouble(conf.get("deltaTimeGap"));
        deltaDocDistribution = Double.parseDouble(conf.get("deltaDocDistribution"));

        // parameters for filter corpora
        historyLength = Integer.parseInt(conf.get("historyLength"));

        // parameters related to event classification supervised learning
        fModel = conf.get("fModel");
        model = libsvm.svm.svm_load_model(fModel);//载入预训练的svm参数，详见java中libsvm包的使用方法
        fSameStoryModel = conf.get("fSameStoryModel");
        sameStoryModel = libsvm.svm.svm_load_model(fSameStoryModel);

        // parameters for query doc matching
        useRelatedNewsTitlesForMatch = Boolean.parseBoolean(conf.get("useRelatedNewsTitlesForMatch"));
        fQueryDocMatchModel = conf.get("fQueryDocMatchModel");
        queryDocMatchModel = libsvm.svm.svm_load_model(fQueryDocMatchModel);
        maxMatchedDocsSize = Integer.parseInt(conf.get("maxMatchedDocsSize"));

    }

}
