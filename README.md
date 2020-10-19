# FixMe
Learning to develop multi-threaded network applications with asynchronous sockets and the java executor framework.

## Getting Started

Have at least JDK 13 and Maven 3.6.3 installed.<br>
Navigate to the clone directory and run the following commands:
```
mvn clean package
java -jar Router\target\Router.jar
java -jar Broker\target\Broker.jar
java -jar Market\target\Market.jar <instrument> <quantity> <price>
```

* The Router should be started first.
* A Broker should then be started.
* Finally start a market with valid arguments in any positive multiple.

## FIX info

https://www.onixs.biz/fix-dictionary/4.2/fields_by_tag.html


