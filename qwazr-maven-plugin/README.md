QWAZR Maven Plugin
==================

A Maven Plugin for [QWAZR](https://www.qwazr.com)

This maven plugin can be used to start or stop a QWAZR application.

Usage
-----

```xml
<plugin>
    <groupId>com.qwazr</groupId>
    <artifactId>qwazr-maven-plugin</artifactId>
    <configuration>
        <etcDirectories>
            <param>/etc/qwazr</param>
        </etcDirectories>
        <etcFilters>
            <param>admin-*</param>
            <param>commons-*</param>
            <param>dev-*</param>
        </etcFilters>
        <dataDirectory>/var/lib/qwazr</dataDirectory>
        <services>
            <param>webapps</param>
            <param>search</param>
            <param>schedulers</param>
            <param>scripts</param>
        </services>
        <listenAddr>0.0.0.0</listenAddr>
        <daemon>true</daemon>
    </configuration>
</plugin>
```

Goals
-----

**Start the QWAZR application**

    mvn qwazr:start

**Stop the QWAZR application**

    mvn qwazr:stop

Parameters
----------

* dataDirectory:
The location of the directory which contains the application.
Default value: the current directory.

* listenAddr:
The local address the server will bind to for TCP connections (HTTP and WebService).

* publicAddr:
The public address the server can be reach with.

* udpAddress:
The local address the server will bind to for UDP connections.

* webappPort:
 The port the server will bind to for HTTP connections. Default is 9090.

* webservicePort:
The port the server will bind to for REST web service connections. Default is 9091.

* udpPort:
The port the server will bind to for UDP connections. Default is 9091.

* webappRealm:
The name of the library item which will handle the Basic authentication for the HTTP connections.

* webserviceRealm:
The name of the library item which will handle the Basic authentication for the REST web service connections.

* etcDirectories:
A list of directories which contains the configuration files.

* etcFilters:
A list of wildcard filters applied to the configuration files.

* services:
A list of services to activate.

* schedulerMaxThreads:
The size of the thread pool used by the scheduler.

* groups:
The groups the application will be registered in.

* daemon:
Pass true to start the QWAZR application as a daemon.
Default value: false.