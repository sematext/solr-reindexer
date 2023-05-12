package org.sematext.solr.reindexer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Reindexer {
    protected static final Logger log = LogManager.getLogger();
    private AtomicBoolean isFinished = new AtomicBoolean(false);

    Context context;
    private BlockingQueue<SolrDocumentList> queue;

    public Reindexer (Context context) {
        this.context = context;
        this.queue = new LinkedBlockingQueue<>(context.intParams.get("queueSize"));
    }

    public void run() {
        Thread readerThread = new Thread(new Reader(queue, context, isFinished));
        readerThread.start();
        ArrayList<Thread> writeThreads = new ArrayList<Thread>();
        for (int i=0; i<context.intParams.get("numWriteThreads"); i++) {
            Thread writerThread = new Thread(new Writer(queue, context, isFinished));
            writerThread.start();
            writeThreads.add(writerThread);
        }

        try {
            readerThread.join();
            log.info("Waiting write threads to empty the queue...");
            for (Thread writeThread : writeThreads) {
                writeThread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static class Reader implements Runnable {
        private BlockingQueue<SolrDocumentList> queue;
        Context context;
        Input input;
        private AtomicBoolean isFinished;

        Reader(BlockingQueue<SolrDocumentList> queue, Context context, AtomicBoolean isFinished) {
            this.queue = queue;
            this.context = context;
            this.input = new Input(context);
            this.isFinished = isFinished;
        }

        @Override
        public void run() {
            try {
                SolrDocumentList page = input.getPage();
                long totalDocs = page.getNumFound();
                long currentPage = 1;
                int rowsPerPage = context.intParams.get("rows");
                long totalPages = (totalDocs + rowsPerPage - 1) / rowsPerPage;

                while ((page != null) && !page.isEmpty()) {
                    // Read from Solr here and add it to the queue
                    log.info("Reading page {} of {}", currentPage, totalPages);

                    queue.put(page);

                    page = input.getPage();
                    currentPage += 1;
                }

                isFinished.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (SolrServerException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class Writer implements Runnable {
        private BlockingQueue<SolrDocumentList> queue;
        Context context;
        Output output;
        private AtomicBoolean isFinished;

        Writer(BlockingQueue<SolrDocumentList> queue, Context context, AtomicBoolean isFinished) {
            this.queue = queue;
            this.context = context;
            this.output = new Output(context);
            this.isFinished = isFinished;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    SolrDocumentList page = queue.poll(); // This will not block, returns null if queue is empty
                    if (page != null) {
                        output.write(page);
                    } else if (isFinished.get()) {
                        break; // exit when done
                    } else {
                        Thread.sleep(50); // If no data, sleep for a while
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (SolrServerException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
