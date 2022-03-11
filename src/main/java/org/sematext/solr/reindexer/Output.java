package org.sematext.solr.reindexer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

import java.io.IOException;
import java.util.*;

public class Output {

    CloudSolrClient client;
    List<String> skipFields = new ArrayList<>();
    Context context;

    protected static final Logger log = LogManager.getLogger();

    public Output(Context context) {
        final List<String> zkServers = new ArrayList<>();
        zkServers.add(context.stringParams.get("zkAddress"));
        client = new CloudSolrClient.Builder(zkServers, Optional.empty())
                .build();

        String[] skipFieldsArray = context.stringParams.get("skipFields").split(",");
        for (String skipField: skipFieldsArray) {
            skipFields.add(skipField);
        }

        this.context = context;
    }

    public void write(SolrDocumentList page) throws SolrServerException, IOException, InterruptedException {
        List<SolrInputDocument> toIndex = new LinkedList<>();

        Iterator<SolrDocument> pageIt = page.iterator();

        while (pageIt.hasNext()) {
            SolrDocument resultDoc = pageIt.next();
            SolrInputDocument inputDoc = new SolrInputDocument();

            for (String name : resultDoc.getFieldNames()) {
                if (!skipFields.contains(name)) {
                    inputDoc.addField(name, resultDoc.getFieldValue(name));
                }
            }

            toIndex.add(inputDoc);
        }

        int retryCount = 1;
        while (true) {
            try {
                client.add(context.stringParams.get("targetCollection"), toIndex);
                break;
            } catch (SolrServerException | IOException e) {
                log.error(e);
                retryCount += 1;
                Thread.sleep(context.intParams.get("retryInterval"));
                if (retryCount > context.intParams.get("retries")) {
                    throw e;
                }
            }
        }

    }
}
