# Data Processing in Financial Services with TomEE (part 1)


This is a joint work of Romain Manni-Buceau and Michal Kalkowski.

The related project is available on github at:

https://github.com/rmannibucau/financial-batch.git


## Overview

The project demonstrates a typical case of maintaining a universe of entities which are taken from an external source.
In this context that would be a list of securities from XETRA market, which are shared on the website as a file and updated daily.
The goal of this batch is to capture these updates and make sure they are reflected in a local database.
Its requirements are similar to those observed in financial services industry and use the best-in-class, cutting-edge technologies.

## Constraints

The batch should be:

- Idempotent.
- Easily restartable, preferrably from the last point of failure.

## Modes of operation

There are several ways to tackle this problem. As part of this post we have implemented first two cases.

1.	Full-streaming: most risky and most efficient for huge amount of data. In this case you have to be sure that the file will be maintained correctly and is not corrupted, i.e. the maintained data-set will not be subject to errors in processing or erroneous size fluctuations. This mode uses chunking with a given commit interval (for performances). This is implemented in the batch called "file-to-database".
2.	2 passes streaming: pre-process the file in order to not corrupt the database if the file is corrupted. Typically that would mean stopping if processing the file would mean deleting more than X% of the rows in the current dataset. Sample implementation in file "semi-streaming" batch.
3.	In memory comparison: load the input data to be processed, subsequently load the full, existing data set and compare the two. The comparison will allow you to make a decision about the relative difference between the two datasets in terms of number of updates, inserts and deletes. This gives you very fined grain decision variables but also enforces you to work in memory. 

## Proof-of-concept

The program is run as a series of loosely-coupled steps which are listed below. The technological stack is described in more details in the next paragraph.

The overall flow is described in this diagram:
 
![Flow](file-to-datase.png)

1.	Download: download the file from remote location and store it as a cache in a defined directory. This is to prevent the file from being re-downloaded in case of failure. It also ensures you can investigate an error if the batch fails (can be due to incoming data).
2.	(Only when using 2 passes) Pre-process: read the input and estimate if the resulting number of deletions will exceed a given threshold.
3.	Process: read the CSV file, transform each line into a POJO (reader). Transform the POJO to a different domain (processor) and synchronize it with the database (writer).
4.	Db-report: log in detail which instruments are new/updated/stale. This is only for investigation and audit purpose.
5.	Release-memory: clean the state which is passed from the steps before. Process and Db-report steps communicate through a shared data in `JobContext`. To avoid to keep these data which can be huge in memory we reset them during this step.
6.	Clean: delete the locally cached file. Note: this is a simple algorithm, in practise we would archive it (mainly a move + renaming).
7.	Report: log some statistics of the execution of each or some steps. This step is mainly for the demo purpose. In practise we would use BatchEE reporting GUI to be able to monitor executions.

## Technology Stack

The program is written using JSR 352 (Batch Processing for Java Platform), introduced in Java EE 7 platform, which provides a programming model to run and manage batches.

We are using BatchEE implementation of the standard which is the implementation you'll find in Apache TomEE for instance.

Additionally we reused some "extra" BatchEE modules. These modules are developped at Apache by BatchEE community but are portable. It means
you can reuse them without BatchEE (using JBeret for instance).
Here is the ones we relied on:

- `batchee-extras`: provide a set of standard API/components. We mainly use from this module the typed version of JBatch component to avoid casting our items (which breaks the code readability).
- `batchee-beanio`: this module integrates JBatch with the excellent BeanIO library. This is the one allowing us to read our input CSV file without any line of code.
- `batchee-modelmapper`: this module integrates JBatch with the ModelMapper library. We rely on this library to onvert our CSV DTO to our JPA entities. Thanks to ModelMapper library this is done without any difficulties thanks to its auto mapping discovery.

### Enter into the code

The most interesting step of our batch is the process one. This one is a chunk. It means it is split in 3 phases:

- a reader responsible to stream data
- then a processor taking reader data and "working" on them. In our case it is just a conversion/mapping.
- and finally a writer responsible to finish the work on the processed data (persistence in our case)

Our process step then looks like:

````xml
<step id="process" next="db-report">
  <chunk>
    <reader ref="org.apache.batchee.beanio.BeanIOReader">
      <properties>
        <property name="file" value="#{jobParameters['downloadCache']}"/>
        <property name="streamName" value="readerCSV"/>
        <property name="configuration" value="beanio.xml"/>
        <property name="skippedHeaderLines" value="5"/>
      </properties>
    </reader>
    <processor ref="org.apache.batchee.modelmapper.ModelMapperItemProcessor">
      <properties>
        <property name="matchingStrategy" value="LOOSE" />
        <property name="destinationType" value="com.supertribe.sample.financial.batch.writer.entity.JpaInstrument" />
      </properties>
    </processor>
    <writer ref="jpa">
        <properties>
          <property name="propagateStatus" value="true" />
        </properties>
    </writer>
  </chunk>
</step>
````

Since we reuse existing components for the reader (`BeanIOReader`) and the processor (`ModelMapperItemProcessor`)
their references are their fully qualified names (note: in batchee you can use short names as well but it is not portable).
Their configuration is straight forward and directly maps the underlying library configuration.

Finally we wrote a custom writer to handle the JPA persistence of our instruments. It is named "jpa" thanks to CDI `@Named`
qualifier. Its `propagateStatus` configuration allows to propagate in the `JobContext` the state of the entities (ie is it a new entry, was it updated, should it be deleted).

Here what our writer looks like:

```java
@Named("jpa")
public class JPAWriter extends NoStateTypedItemWriter<JpaInstrument> {
    @PersistenceContext
    private EntityManager em;

    @Inject
    private JobContext context;

    @Inject
    @BatchProperty
    private Boolean propagateStatus;

    private Map<JpaInstrument.Id, Item<JpaInstrument>> all;

    @PostConstruct
    private void cacheDb() { // supposes the batch is the only one to write for its execution duration, acceptable for reference db
        // avoid em.getReference(JpaInstrument.class, instrument.getId()); in doWriteItems and N db hits
        all = em.createNamedQuery("JpaInstrument.findAll", JpaInstrument.class)
                .getResultList().stream()
                .map(i -> new Item<>(i, null))
                .collect(toMap(i -> i.value.getId(), identity()));
    }

    @Override
    protected void doWriteItems(final List<JpaInstrument> list) {
        list.stream().forEach(instrument -> {
            if (Optional.ofNullable(all.get(instrument.getId())).isPresent()) {
                em.merge(instrument);
                all.put(instrument.getId(), new Item<>(instrument, false));
            } else {
                em.persist(instrument);
                all.put(instrument.getId(), new Item<>(instrument, true));
            }
        });
        if (propagateStatus != null && propagateStatus) {
            context.setTransientUserData(all); // allow next step to reuse it if needed
        }
    }

    public static class Item<T> { // skipping constructor and getters for readability
        private final T value;
        private Boolean isNew;
    }
}
```

Interesting thins to note about this writer are:

- It is a plain CDI bean so we can get injections as in any bean
- It uses JBatch configuration thanks to `@BatchProperty` qualifier
- it uses a query to preload in memory all the database to have a faster runtime processing. Note: if there is a natural ordering on the dataset on both sides (input and database) it can be optimized loading only the chunk data (ie loading only data for a single commit interval).
- It uses `JobContext#setTransientData` to share data with next step(s)


## Advantages

A sure advantage of this kind of programming model is the ability to easily test the units, steps and finally jobs independently.
The responsibility of each component is clearly limited and its purpose gives a clear indication of its purpose, i.e. readers, processors, writers and listeners. It really allows
reusability (like we did with `BeanIOReader` for instance).

Another strong point is the freedom of changing say input or output of the flow without impacting the whole processing logic.
Moving from database to a queue requires the change of only one, clearly defined component. The maintenance of such a system is surely less costly than custom alternatives.
It also allows to restart a batch to a particular step - this is a feature of JBatch - without having to write another batch rewiring all parts of the batch.

Performance is also much easier to achieve given that you have an easy way of implementing pipelining and chunking which is rarely well done manually.
However JBatch doesn't prevent you to customize the commit interval algorithm which allows you to have very advanced logic here.

We didn't speak a lot of it too but JBatch provides you listeners for all part of the batch (Job, Step, Reader, Processor, Writer, ...). This allows you
to audit but also customize very easily your batch without impacting the component programming model.

## Error Handling

There are several strategies of handling the runtime errors in a batch, and whichever you choose, it has to be stated upfront and communicated clearly.
Errors may result from several problems - malformed data, network errors, failed business validation etc.  It is recommended to simulate how the application will behave in case of each error.

Side note: we'll not enter into the details in this post but BatchEE provides a test module simplifying the testing of steps.

1.	All or nothing: hard to achieve as it basically means you have to pass all the entities through the whole chain (including DB persistence), and rollback all in case of an error.
2.	Best-effort: even if an error occurs, continue. This is typical if you want. In this case you just ensure the failure is visible (in BatchEE GUI for instance or sending a mail at the end of the batch).
3.	Stop on error: once the error is spotted, the application stops with a FAILED status. This results in a potentially partially-complete execution. This type is used in the provided sample.

The way errors will be redelivered or mitigated is not part of this categorization. The two main cases are:

- auto-retry: can be done with EIP Dead Letter Channel pattern
- human handling: ensure a human is notified and stay in fail status until it is solved (often means restarted and succeed).

## Sample executions

Just to make things a big concrete here are a simple execution logs. It was collected using BatchEE CLI which basically allows you
to run batches from the command line. Interesting point is it supports execution of batches from a war.

    $ ./bin/batchee start \
            -lifecycle openejb \
            -archive financial-batch.war \
            -name file-to-database \
            inputURL=http://www.xetra.com/blob/1424940/cdbb8e95489e25f891f537f70375fb04/data/allTradableInstruments.csv \
            downloadCache=work/cache.csv
    
    (...)
    
    31-mar-2015 21:59:15.429 INFO [main] org.apache.webbeans.config.BeansDeployer.validateInjectionPoints All injection points were validated successfully.
    31-mar-2015 21:59:15.434 INFO [main] org.apache.openejb.cdi.OpenEJBLifecycle.startApplication OpenWebBeans Container has started, it took 316 ms.
    31-mar-2015 21:59:15.443 INFO [main] org.apache.openejb.assembler.classic.Assembler.createApplication Deployed Application(path=c:\apps\apache-batchee-0.3-incubating-SNAPSHOT\work\financial-batch-1.0-SNAPSHOT.war)
    
     ____        _       _     ______ ______
    |  _ \      | |     | |   |  ____|  ____|
    | |_) | __ _| |_ ___| |__ | |__  | |__
    |  _ < / _` | __/ __| '_ \|  __| |  __|
    | |_) | (_| | || (__| | | | |____| |____
    |____/ \__,_|\__\___|_| |_|______|______|0.3-incubating-SNAPSHOT
    Admin mode deactivated, use -socket to activate it
    Batch 'file-to-database' started with id #0
    
    (....)
    
    
    31-mar-2015 21:54:13.225 INFO [batchee-thread-1] com.supertribe.sample.financial.batch.writer.JPAReport.lambda$logStream$4   - JpaInstrument{id=Id{isin='ZAG000106972', mic='XFRA', currency='EUR'}}
    31-mar-2015 21:54:13.226 INFO [batchee-thread-1] com.supertribe.sample.financial.batch.writer.JPAReport.lambda$logStream$4   - JpaInstrument{id=Id{isin='ZAG000106998', mic='XFRA', currency='EUR'}}
    31-mar-2015 21:54:13.226 INFO [batchee-thread-1] com.supertribe.sample.financial.batch.writer.JPAReport.lambda$logStream$4   - JpaInstrument{id=Id{isin='ZAG000107004', mic='XFRA', currency='EUR'}}
    31-mar-2015 21:54:13.227 INFO [batchee-thread-1] com.supertribe.sample.financial.batch.writer.JPAReport.lambda$logStream$4   - JpaInstrument{id=Id{isin='ZAG000107012', mic='XFRA', currency='EUR'}}
    31-mar-2015 21:54:13.238 INFO [batchee-thread-1] com.supertribe.sample.financial.batch.report.Report.doReport Step: process
    31-mar-2015 21:54:13.238 INFO [batchee-thread-1] com.supertribe.sample.financial.batch.report.Report.doReport Execution Id: 1
    31-mar-2015 21:54:13.239 INFO [batchee-thread-1] com.supertribe.sample.financial.batch.report.Report.doReport Status: COMPLETED
    31-mar-2015 21:54:13.239 INFO [batchee-thread-1] com.supertribe.sample.financial.batch.report.Report.doReport Start: Tue Mar 31 21:52:57 CEST 2015
    31-mar-2015 21:54:13.240 INFO [batchee-thread-1] com.supertribe.sample.financial.batch.report.Report.doReport Stop: Tue Mar 31 21:53:03 CEST 2015
    31-mar-2015 21:54:13.240 INFO [batchee-thread-1] com.supertribe.sample.financial.batch.report.Report.doReport Metrics:
    31-mar-2015 21:54:13.243 INFO [batchee-thread-1] com.supertribe.sample.financial.batch.report.Report.lambda$doReport$9   - COMMIT_COUNT = 2
    31-mar-2015 21:54:13.243 INFO [batchee-thread-1] com.supertribe.sample.financial.batch.report.Report.lambda$doReport$9   - FILTER_COUNT = 0
    31-mar-2015 21:54:13.244 INFO [batchee-thread-1] com.supertribe.sample.financial.batch.report.Report.lambda$doReport$9   - PROCESS_SKIP_COUNT = 0
    31-mar-2015 21:54:13.244 INFO [batchee-thread-1] com.supertribe.sample.financial.batch.report.Report.lambda$doReport$9   - READ_COUNT = 15
    31-mar-2015 21:54:13.245 INFO [batchee-thread-1] com.supertribe.sample.financial.batch.report.Report.lambda$doReport$9   - READ_SKIP_COUNT = 0
    31-mar-2015 21:54:13.246 INFO [batchee-thread-1] com.supertribe.sample.financial.batch.report.Report.lambda$doReport$9   - ROLLBACK_COUNT = 0
    31-mar-2015 21:54:13.246 INFO [batchee-thread-1] com.supertribe.sample.financial.batch.report.Report.lambda$doReport$9   - WRITE_COUNT = 15
    31-mar-2015 21:54:13.247 INFO [batchee-thread-1] com.supertribe.sample.financial.batch.report.Report.lambda$doReport$9   - WRITE_SKIP_COUNT = 0
    
    =========================
    Batch status: COMPLETED
    Exit status:  COMPLETED
    Duration:     5s
    =========================
    31-mar-2015 21:54:13.252 INFO [main] org.apache.openejb.assembler.classic.Assembler.destroyApplication Undeploying app: c:\apps\apache-batchee-0.3-incubating-SNAPSHOT\work\financial-batch-1.0-SNAPSHOT.war
    31-mar-2015 21:54:13.550 INFO [main] org.apache.openejb.util.ServiceManagerProxy.stop Stopping network services
    31-mar-2015 21:54:13.550 INFO [main] org.apache.openejb.server.SimpleServiceManager.stop Stopping server services
    31-mar-2015 21:54:13.553 INFO [main] org.apache.openejb.core.LocalInitialContext.tearDownOpenEJB Destroying container system
    31-mar-2015 21:54:13.562 INFO [main] org.apache.openejb.assembler.classic.Assembler.destroyResource Closing DataSource: My DataSource
    31-mar-2015 21:54:13.562 INFO [main] org.apache.openejb.assembler.classic.Assembler.destroyResource Closing DataSource: My Unmanaged DataSource
    31-mar-2015 21:54:13.575 INFO [main] org.apache.openejb.assembler.classic.Assembler.destroyResource Closing DataSource: jdbc

The first part of the logs are the container logs (deployment of the application, setup of CDI/EJB/... etc).
Then after the BatchEE ascii logo the batch starts and we can identify the report step logging (`JPAReport`) and the step reporting (`Report`).
Finally BatchEE report the whole job status (`COMPLETED` for this execution) before the container shutdowns.


And now for something completely different, a failure of download. Stage at which error occured is clearly visible:

    =========================
    Batch status: FAILED
    Exit status:  FAILED
    Duration:     21s
    
    Step name       : download
    Step status     : FAILED
    Step exit status: FAILED
    =========================


## Conclusions

This post was mainly an overview of what JBatch can do and how it can smoothly be integrated in today's financial systems.

However there are a lot of other features we didn't fully cover but are here and can be useful for production like:
 
- restart of a failed batch
- how to handle stopping of a batch gracefully
- testing
- webapp GUI, JAXRS API
- and much more...

Stay tuned!
