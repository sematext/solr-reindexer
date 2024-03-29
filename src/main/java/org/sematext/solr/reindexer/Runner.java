package org.sematext.solr.reindexer;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;

import java.io.IOException;

public class Runner {
    protected static final Logger log = LogManager.getLogger();

    static Context context = new Context();

    static CommandLine cmd;

    public static void main(String args[]) throws SolrServerException,
            IOException, InterruptedException, ParseException {

        parseCmdLine(args);

        Reindexer reindexer = new Reindexer(context);
        reindexer.run();
        log.info("Done!");
        System.exit(0);
    }

    private static void parseCmdLine(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption("help", "Print this message");
        options.addOption(Option.builder(	"rows")
                .desc( "Number of rows per page. Defaults to 1000" )
                .hasArg(true)
                .build());
        options.addOption(Option.builder(	"retries")
                .desc( "Number of retries if we fail to talk to Solr. Defaults to 10" )
                .hasArg(true)
                .build());
        options.addOption(Option.builder(	"numWriteThreads")
                .desc( "Number of write threads. Defaults to 2" )
                .hasArg(true)
                .build());
        options.addOption(Option.builder(	"queueSize")
                .desc( "Maximum size of the in-memory queue between reading and writing (number of pages). Defaults to 100" )
                .hasArg(true)
                .build());
        options.addOption(Option.builder(	"retryInterval")
                .desc( "Interval between retries in milliseconds. Defaults to 5000" )
                .hasArg(true)
                .build());
        options.addOption(Option.builder(	"sourceZkAddress")
                .desc( "Zookeeper addresses for source SolrCloud. Defaults to 'localhost:2181'. Comma-separate multiple addresses" )
                .hasArg(true)
                .build());
        options.addOption(Option.builder(	"sourceShards")
                .desc( "Shard to query (normally, we query all). Comma-separate multiple shards" )
                .hasArg(true)
                .build());
        options.addOption(Option.builder(	"targetZkAddress")
                .desc( "Zookeeper addresses for target SolrCloud. Defaults to 'localhost:2181'. Comma-separate multiple addresses" )
                .hasArg(true)
                .build());
        options.addOption(Option.builder(	"query")
                .desc( "Query for fetching data. Defaults to *:*" )
                .hasArg(true)
                .build());
        options.addOption(Option.builder(	"uniqueKey")
                .desc( "uniqueKey field to sort by. Defaults to 'id'" )
                .hasArg(true)
                .build());
        options.addOption(Option.builder(	"sourceCollection")
                .desc( "Collection to reindex from (required)" )
                .required()
                .hasArg(true)
                .build());
        options.addOption(Option.builder(	"targetCollection")
                .desc( "Collection to reindex to (required)" )
                .required()
                .hasArg(true)
                .build());
        options.addOption(Option.builder(	"skipFields")
                .desc( "Fields to skip while reindexing. Defaults to _version_. Comma-separate multiple fields" )
                .hasArg(true)
                .build());

        CommandLineParser parser = new DefaultParser();
        cmd = parser.parse(options, args);

        if(cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("reindexer", options);
            System.exit(0);
        }

        setIntegerParam("rows", 1000);
        setIntegerParam("retries", 10);
        setIntegerParam("numWriteThreads", 2);
        setIntegerParam("queueSize", 100);
        setIntegerParam("retryInterval", 5000);
        setStringParam("sourceZkAddress", "localhost:2181");
        setStringParam("sourceShards", null);
        setStringParam("targetZkAddress", "localhost:2181");
        setStringParam("query", "*:*");
        setStringParam("uniqueKey", "id");
        setStringParam("sourceCollection", null);
        setStringParam("targetCollection", null);
        setStringParam("skipFields", "_version_");
    }

    public static void setIntegerParam(String paramName, int defaultValue) {
        int paramValue = defaultValue;
        String paramStringValue = cmd.getOptionValue(paramName);
        if (paramStringValue != null) {
            paramValue = Integer.parseInt(paramStringValue);
        }
        context.intParams.put(paramName, paramValue);
    }

    public static void setStringParam(String paramName, String defaultValue) throws ParseException {
        String paramValue = cmd.getOptionValue(paramName);
        if (paramValue == null) {
            paramValue = defaultValue;
        }
        context.stringParams.put(paramName, paramValue);
    }
}
