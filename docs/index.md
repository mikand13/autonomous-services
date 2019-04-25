## Welcome to Autonomous Services

![CodeBuild badge](https://codebuild.eu-west-1.amazonaws.com/badges?uuid=eyJlbmNyeXB0ZWREYXRhIjoiNXV5Tm4rc0MwRGhIczd5eHArK3JiUU1OYzNVcjhGWVNzUHlBSjRzTzJuU2FXcmNvQU51TXlIb2hZRkI5TUdCdThoczFqOElEZTV0dXBhdVNoYmFlM3p3PSIsIml2UGFyYW1ldGVyU3BlYyI6IjBFRDhzUFNjQ2xxc1RuUEYiLCJtYXRlcmlhbFNldFNlcmlhbCI6MX0%3D&branch=master)

This is the implementation part of a Master Thesis. It is located [here](https://drive.google.com/file/d/16Nwfh8wMR5eMf7PJ9150F9zSvDUvR4Mz/view?usp=sharing) and this is a brief explanation. A comprehensive description of Autonomous Services is found in the thesis. 

Autonomous Services are Design Principles for Asynchronous and Agnostic Microservice Architectures. It leverages [vert.x](https://github.com/vert-x3) and [nannoq-tools](https://github.com/NoriginMedia/nannoq-tools) to provide an event-based reactive architecure without centralized components, netiher for communication or data, providing a theoretically linear scalability across the architecture.

This repo is a collection of the most current version of all autonomous-services modules.

### Prerequisites

Vert.x >= 3.5.3

Java >= 1.8

Kotlin

Gradle

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
    <version>1.0.1</version>
</dependency>
```

or Gradle:

```groovy

dependencies {
    compile 'org.mikand.autonomous.services:module-name:1.0.1'
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
