package org.sematext.solr.reindexer;

import org.apache.logging.log4j.LogManager;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CursorMarkParams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.Logger;

public class Input {
    CloudSolrClient client;
    String cursorMark = CursorMarkParams.CURSOR_MARK_START;
    SolrQuery q;
    boolean done = false;
    Context context;
    protected static final Logger log = LogManager.getLogger();

    public Input(Context context) {
        final List<String> zkServers = new ArrayList<>();
        String[] zkAddresses = context.stringParams.get("sourceZkAddress").split(",");
        for (String zkAddress: zkAddresses) {
            zkServers.add(zkAddress);
        }
        client = new CloudSolrClient.Builder(zkServers, Optional.empty())
                .build();
        q = new SolrQuery(context.stringParams.get("query"));

        String uniqueKey = context.stringParams.get("uniqueKey");
        q.setSort(SolrQuery.SortClause.asc(uniqueKey));

        q.setRows(context.intParams.get("rows"));
        this.context = context;
    }

    public SolrDocumentList getPage() throws SolrServerException, IOException, InterruptedException {
        if (done) {
            return null;
        } else {
            q.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
            QueryResponse rsp;

            int retryCount = 1;
            while (true) {
                try {
                    rsp = client.query(context.stringParams.get("sourceCollection"), q);
                    String nextCursorMark = rsp.getNextCursorMark();
                    if (cursorMark.equals(nextCursorMark)) {
                        done = true;
                    }
                    cursorMark = nextCursorMark;
                    return rsp.getResults();
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

}
