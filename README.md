## Welcome to Nannoq Tools

[![Build Status](https://www.tomrom.net/buildStatus/icon?job=autonomous-services/master)](https://www.tomrom.net/job/autonomous-services/job/master/)

This repo is a collection of the most current version of all autonomous-services modules.

### Prerequisites

Vert.x >= 3.5.0

Java >= 1.8

Maven

## Installing

./gradlew install

### Running the tests

./gradlew test

## Usage

First install with either Maven:

```xml

<dependency>
    <groupId>org.mikand.autonomous.services</groupId>
    <artifactId>module-name</artifactId>
    <version>1.0.0</version>
</dependency>
```

or Gradle:

```groovy

dependencies {
    compile 'org.mikand.autonomous.services:module-name:1.0.0'
}
```

### Implementation and Use

Please consult the individual modules on implementations and use, this is just a parent project.

## Contributing

Please read [CONTRIBUTING.md](https://github.com/mikand13/autonomous-services/blob/master/CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/mikand13/autonomous-services/tags)

## Authors

* **Anders Mikkelsen**

See also the list of [contributors](https://github.com/mikand13/autonomous-services/contributors) who participated in this project.

## License

This project is licensed under the MIT License - see the [LICENSE.md](https://github.com/mikand13/autonomous-services/blob/master/LICENSE) file for details
