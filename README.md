# Skeleton process repository for the 52°North WPS 4.x 
Skeleton project for creating new process repositories (also called backends) for the 52°North WPS version 4.x

## Features

This is an example repository for the 52°North WPS version 4.x. You can use it as a starting point to develop your own process repository (also called backend). 
A repository basically consists of two classes. The repository class that handles the algorithm instantiation. And the configuration module that handles the initiation of the repository and the repository settings. 

A repository can handle all algorithms that extend [AbstractAlgorithm](https://github.com/52North/WPS/blob/wps-4.0/52n-wps-algorithm/src/main/java/org/n52/wps/server/AbstractAlgorithm.java). Sometimes you will need to create process delegators and generic processes, because your functionality is not written in Java (see e.g. the [GRASS 7 algorithm repository](https://github.com/52North/WPS/tree/wps-4.0/52n-wps-grass/src/main/java/org/n52/wps/server/grass)). The [GenericExampleAlgorithm](https://github.com/bpross-52n/wps-process-repository-skeleton/blob/master/src/main/java/org/n52/geoprocessing/example/algorithm/GenericExampleAlgorithm.java) is an example for this.

## Usage

Simple build the example repository using 

```
$ mvn clean install
```

Just drop the process-repository-skeleton-X.X.jar in the WEB-INF/lib folder of your WPS 4.x webapp. 
Now, you will have to modify the file WEB-INF/classes/dispatcher-servlet.xml.
Change line 47 from 

```
<context:component-scan base-package="org.n52.wps">
```

to 

```
<context:component-scan base-package="org.n52">
```

Now the repository should be automatically added to the WPS configuration. To add the example process, you can use the admin interface.
Log in and navigate to the *Repositories* tab. The *ExampleAlgorithmRepository* should be listed there. Extend the repository and you should see a button *Add algorithm*.
Click on it and a dialog should appear. Add the following class name:

```
org.n52.geoprocessing.example.algorithm.GenericExampleAlgorithm
```

After you restart the WPS the process should be available.
