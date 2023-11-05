# data-space

This software encompasses capabilities such as data source adaptation, data ingestion, data submission, and access permission management. On data source adaptation, it integrates and encapsulates various data sources, including file systems, databases, and web resources, and graph databases and allow data stewards customize data sources. On data ingestion, it facilitates both online and offline access to multi-source heterogeneous scientific data. On access permission, it enables the creation of separate data spaces for different projects or organizations, supporting user role assignments for collaborative multi-user efforts.

## build & install
```shell
mvn clean compile install 
```

##  modify start.sh
```
open ./start.sh
modifying startup parameters
-DmongoHost mongo database address
-DftpPort  ftp external service port
-DDB_HOST   mysql database address
-DSQL_ROOT_PATH mysql init script location
```

4. start
```shell
sh start.sh
```
5. view log
```shell
tail -f ./api.log
```
6. stop server
```shell
sh stop.sh
```

