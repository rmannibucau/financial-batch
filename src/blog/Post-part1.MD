# Data Processing in Financial Services with TomEE (part 1)


This is a joint work of Romain Manni-Buceau and Michal Kalkowski.

The related project is available on github at:

https://github.com/rmannibucau/financial-batch.git


## Overview
The project demonstrates a typical case of maintaining a universe of entities which are taken from an external source. In this context that would be a list of securities from XETRA market, which are shared on the website as a file and updated daily. The goal of this batch is to capture these updates and make sure they are reflected in a local database. Its requirements are similar to those observed in financial services industry and use the best-in-class, cutting-edge technologies.

## Constraints
The batch should be:
- Idempotent.
- Easily restartable, preferrably from the last point of failure.

## Modes of operation
There are several ways to tackle this problem. As part of this post we have implemented first two cases.
1.	Full-streaming – most risky and most efficient. In this case you have to be sure that the file will be maintained correctly, i.e. the maintained data-set will not be subject to errors in processing or erroneous size fluctuations. Uses chunking with a given commit interval. This is implemented in the batch called ”file-to-database.xml”
2.	Semi-streaming – pre-process the file in order to preserve the size of the dataset. Typically that would mean stopping if processing the file would mean deleting more than X % of the rows in the current dataset. Sample implementation in file “semi-streaming.xml”
3.	Custom-checkpoint – aggregate until the condition of safety threshold is met. This results in the first chunk being large, which would probably require splitting it further to avoid having large DB redo log.
4.	Full-load – load the input data to be processed, subsequently load the full, existing data set and compare the two. The comparison will allow you to make a decision about the relative difference between the two datasets in terms of number of updates, inserts and deletes. Stay tuned … 

## Proof-of-concept
The program is run as a series of loosely-coupled steps which are listed below. The technological stack is described in more details in the next paragraph.

1.	Download – download the file from remote location and store it as a cache in a defined directory. This is to prevent the file from being re-downloaded in case of failure. The importance of having this step is to be able to quickly do some adjustments if necessary.
2.	(Only in semi-streaming) Pre-process – read the input and estimate if the resulting number of deletions will exceed a given threshold.
3.	Process – read the CSV file, transform each line into a POJO. Transform the POJO to a different domain and synchronize it with the database.
4.	Db-repot – log in detail which instruments are new/updated/stale.
5.	Release-memory – clean the state which is passed from the steps before.
6.	Clean – delete the locally cached file.
7.	Report – log the details of the execution of each step.
	
Extract from the Process pipeline:
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

## Technology Stack
The program is written using JSR 352 (Batch Processing for Java Platform), introduced in Java EE 7 platform, which programming model and a runtime to run and manage batch jobs. We are using BatchEE implementation of the standard.

BeanIO is an open-source framework for reading and writing POJOs from data streams, i.e. CSV/XML file. The input files in financial industries more often than not have a large number of columns, from which you need only a portion. To avoid the daunting work of defining them all in your POJO, the library allows you to state that you need fields 1 and 90. Moreover, it allows you to define your own custom type converters, i.e. BigDecimal with special rounding. Moreover it is well-integrated with JSR 352.

ModelMapper is a library which aims to simplify object-to-object mapping. In the project it maps the CSV domain to JPA one. It can handle a great part of work automatically.

## Advantages
A sure advantage of this kind of programming model is the ability to easily test the units, steps and finally jobs independently. The responsibility of each component is clearly limited and its purpose gives a clear indication of its purpose, i.e. readers, processors, writers and listeners.

Another strong point is the freedom of changing say input or output of the flow without impacting the whole processing logic. Moving from database to a queue requires the change of only one, clearly defined component. The maintenance of such a system is surely less costly than custom alternatives.

Performance is also much easier to achieve given that you have an easy way of implementing pipelining. JSR 352 gives you a good deal of tools to process the data in chunks, making sure full-load is only done when the user clearly wants so.

## Error Handling
There are several strategies of handling the runtime errors in a batch, and whichever you choose has to be stated upfront and communicated clearly. Errors may result from several problems – malformed data, network errors, failed business validation etc.  It is recommended to simulate how the application will behave in case of each error.
1.	All or nothing – hard to achieve as it basically means you have to pass all the entities through the whole chain (including DB persistence), and rollback all in case of an error.
2.	Best-effort – even if an error occurs, continue. This is typical if you want
3.	Stop on error – once the error is spotted, the application stops with a FAILED status. This results in a potentially partially-complete execution. This type is used in the provided sample.
The way errors will be redelivered or mitigated is not part of this categorization. It may be handled typically with EIP Dead Letter Channel pattern.

## Sample executions
----
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

----


And now for something completely different, a failure of download. Stage at which error occured is clearly visible:

----

    =========================
    Batch status: FAILED
    Exit status:  FAILED
    Duration:     21s

    Step name       : download
    Step status     : FAILED
    Step exit status: FAILED
    =========================

----


## Conclusions
This post has presented a several manners in which data processing can be executed. The discussed samples present processing of large amounts of data in a standardized way.

