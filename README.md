Serenity TeamCity StepListener
========================================
[![Build Status](https://secure.travis-ci.org/vase4kin/serenity-teamcity-steplistener.png?branch=master)](https://travis-ci.org/vase4kin/serenity-teamcity-steplistener)
[![Coverage Status](https://coveralls.io/repos/vase4kin/serenity-teamcity-steplistener/badge.png?branch=master)](https://coveralls.io/r/vase4kin/serenity-teamcity-steplistener?branch=master)

`Serenity TeamCity StepListener` is extension for integration [Serenity](http://thucydides.info/) and [TeamCity](http://www.jetbrains.com/teamcity/) continious integration server. Based on Serenity's StepListener interface implementation and TeamCity's Service Messages providing fast and easy test reporting during build process.

Requirements
------------

* TeamCity 7.0+
* Serenity (core version 1.0.45+)

Installation
------------

Simply add a dependency to your project's pom file:

        <dependency>
            <groupId>com.github.vase4kin</groupId>
            <artifactId>serenity-teamcity-steplistener</artifactId>
            <version>1.0.0</version>
        </dependency>
        
####JBehave:
        
Add a configuration parameter to your maven-failsafe-plugin:

        <disableXmlReport>true</disableXmlReport>
        
For example like this:

        <plugin>
                <artifactId>maven-failsafe-plugin</artifactId>
                <version>2.11</version>
                <configuration>
                    <disableXmlReport>true</disableXmlReport>
                </configuration>
        </plugin>            

####JUnit:

Add a configuration parameters to your maven-surefire-plugin:

        <disableXmlReport>true</disableXmlReport>
        <skip>false</skip>
        
        
For example like this:

        <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>2.12</version>
                <configuration>
                    <disableXmlReport>true</disableXmlReport>
                    <skip>false</skip>
                </configuration>
        </plugin>           

Documentation
-------------

####JBehave: 
Run with `mvn integration-test` command

####JUnit:
Run with `mvn test` command

####TeamCity:
Press "Run..." button in TeamCity.
Now build will display executed tests in realtime in "Overview" screen
