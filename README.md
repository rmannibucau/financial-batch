# financial-batch

## Build

1. set JAVA_HOME to a jdk 1.8
2. execute:


    mvn clean package

## Execute in standalone:

1. get apache-batchee-openejb-0.3-incubating-SNAPSHOT.zip
2. unzip it and go in the extracted folder
3. run:


    # optional but for debugging (see sql statements)
    export BATCHEE_OPTS="-Djdbc.LogSql=true -Djdbc=new://Resource?type=DataSource $BATCHEE_OPTS"
    
    # to use disk database
    export BATCHEE_OPTS="-Djdbc.JdbcUrl=jdbc:hsqldb:file:database -Djdbc=new://Resource?type=DataSource $BATCHEE_OPTS"
    
    ./bin/batchee start -lifecycle openejb -archive financial-batch-1.0-SNAPSHOT.war \
        -name file-to-database \
        inputURL=http://www.xetra.com/blob/1424940/cdbb8e95489e25f891f537f70375fb04/data/allTradableInstruments.csv \
        downloadCache=/tmp/cache.csv


## Note

Data source: http://www.xetra.com/blob/1424940/cdbb8e95489e25f891f537f70375fb04/data/allTradableInstruments.csv
