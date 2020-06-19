[![Build Status](https://travis-ci.org/matsim-vsp/opt-drt.svg?branch=master)](https://travis-ci.org/matsim-vsp/opt-drt)

# opt-drt project

The idea of this module is to have DRT operators which dynamically adjust their service based on predefined decision criteria (e.g. waiting time percentiles, number of DRT users, profit). There are several DRT service adjustment strategies:
* Adjustment of the vehicle fleet size
* Adjustment of the service area
* Adjustment of the distance-based fare (fare surcharge)

This tool may be used to make sure that service quality parameters, in particular the waiting times, remain constant throughout a simulation run in which DRT demand levels may change. The tool may also be used to improve the existing DRT service, for example to identify a 'good' service area or fleet size to increase profit or the number of DRT users (experimental).

If you want to play around with the module, have a look into the run example and test classes to see how to modify your MATSim configuration. If you want to use the opt-drt module as a maven-dependency in your project add the following to your pom file:

```xml
<repository>
  <id>jitpack.io</id>
  <url>https://jitpack.io</url>
</repository>
```

```xml
<dependency>
 <groupId>com.github.matsim-vsp</groupId>
 <artifactId>opt-drt</artifactId>
 <version>v2.3</version>
</dependency>
```
 
