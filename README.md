# AOP DEMO

AOP is a technique that promotes separation of concerns by providing additional capabilities to components that already
contain a specific responsibility.

Please read [THIS](https://docs.spring.io/spring-framework/docs/2.5.0/reference/aop.html) to have a complete
understanding of AOP.

This project presents 2 different approaches to use AOP:

* The first approach is exhaustively cited in articles and tutorials out there, it determines how to log the input and
  output of any desired method.
* The second approach is more complex. Using AOP to encode/decode number ids using [Hashids](https://hashids.org/).

## Getting started

The project is a restfull Spring Boot app that persist the data with the
in-memory [H2](https://www.h2database.com/html/main.html) database.

## How to Run the project

Once you clone the project to your local machine, run the command below from the root folder of the project:

```shell
mvn spring-boot:run
```

You can then get the list of students with the command bellow:

```shell
curl -X GET http://localhost:8080/students
```

## Scenario #1: Logging method input and output

### 1. Annotation

Create a new annotation that will be used to determine which methods or classes will be logging:

```java

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogRequestResponse {

}
```

### 2. Aspect component

Create a new component:

```java

@Component
@Aspect
@Order(1)
@Slf4j
public class LogRequestResponseAspect {
  //...
}
```

The class is annotated with the following:

* **@Component** to turn this into a spring component;
* **@Aspect** to identify the component as aspect;
* **@Order** to determine in which order the aspect will be executed (when more than one is created);
* **@Slf4j** Lombok annotation to inject log capabilities.

Now it's time to establish the predicates that will enforce the aspect execution:

```java
@Pointcut("within(@com.marcelocastro.util.LogRequestResponse *)")
public void beanAnnotated(){}

@Pointcut("execution(public * *(..))")
public void publicMethod(){}

@Pointcut("beanAnnotated() && publicMethod()")
public void publicMethodsWithinAnnotatedClass(){}

@Pointcut("@annotation(com.marcelocastro.util.LogRequestResponse)")
public void annotatedMethod(){}
```

It has basically two predicates: (1) any method annotated with @LogRequestResponse **or** (2) any public method of a
class annotated with @LogRequestResponse.

It's time to define the advice:

```java
@Around("annotatedMethod() || publicMethodsWithinAnnotatedClass()")
public Object logExecutionTime(ProceedingJoinPoint joinPoint)throws Throwable{
  log.info(joinPoint.getSignature()+" Request arguments: {}",joinPoint.getArgs());
  Object proceed=joinPoint.proceed();
  log.info(joinPoint.getSignature()+" Response: {} ",Objects.isNull(proceed)?"{}":proceed);
  return proceed;
}
```

Because the advice is of type @Around, the methods has the opportunity to log information before and after the
join point.

Finally, the StudentController class will be annotated with @LogRequestResponse, making all public methods eligible to
execute the aspect log:

```java

@RestController
@RequestMapping("/students")
@LogRequestResponse
@AllArgsConstructor
public class StudentController {

}
```

When the list of students is called:

``` 
curl -X GET http://localhost:8080/students
```

The log shows the request and response of the method in the annotated controller class:

```
c.m.aspect.LogRequestResponseAspect      : List com.marcelocastro.controller.StudentController.listAllStudents() Request arguments: {}
c.m.aspect.LogRequestResponseAspect      : List com.marcelocastro.controller.StudentController.listAllStudents() Response: [StudentDto(id=1, firstName=Michael, lastName=Jordan, email=michael.jordan@nba.com), StudentDto(id=2, firstName=Magic, lastName=Johnson, email=magic.johnson@nba.com), StudentDto(id=3, firstName=Larry, lastName=Bird, email=larry.bird@nba.com)]
```

## Scenario 2: Using Hashids to encode/decode id

The id of each student is stored in the database as integer and auto_increment. Here's the table schema:

```sql 
CREATE TABLE IF NOT EXISTS student (
    id INTEGER AUTO_INCREMENT  PRIMARY KEY,
    first_name VARCHAR(250) NOT NULL,
    last_name VARCHAR(250) NOT NULL,
    email VARCHAR(250) NOT NULL
);
```

However, it is reasonable to expect that sometimes the id number shouldn't be exposed, as it can become easy to
determine any student by simply modifying the number id. That's when [Hashids](https://hashids.org/) might become handy.
In this project, Hashids and AOP are working together to encode/decode the id in the request/response.

### 1. Create an interface

This interface is going to help the Aspect code to quickly identify a class that might contain the id to be
encode/decoded.

```java
public interface Hasheable {

}
```

### 2. Create an annotation

The annotation will be used to identity the property/parameter to be encoded/decoded

```java

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Hashids {

  Domain domain();
}
```

The ```Domain``` is an enum containing the prefix for the student or any other domain in the system (Teachers,
Discipline, etc). This prefix is going to be used to for part of the encoded number. Something like ```STD-KZB5B18Y```

### 3. Create Aspect component

Create a new component:

```java
@Aspect
@Order(1)
@Component
@Slf4j
public class HashidsAspect {
}
```
The class is annotated with the following:

* **@Component** to turn this into a spring component;
* **@Aspect** to identify the component as aspect;
* **@Order** to determine in which order the aspect will be executed (when more than one is created);
* **@Slf4j** Lombok annotation to inject log capabilities.

It's time to define the advice:

```java
@Around("within(com.marcelocastro..controller..*)")
public Object applyHashids(ProceedingJoinPoint joinPoint) throws Throwable {
  Object[] args = applyHashidsInParameters(joinPoint);
  List<Object> argsHashed = Arrays.stream(args)
    .map(this::applyHashids)
    .toList();
  Object proceed = joinPoint.proceed(argsHashed.toArray());
  applyHashids(proceed);
  return proceed;
}
```
Because the advice is of type @Around, the method has the opportunity to capture the original data from the join point, 
determine if there's a parameter annotated with @Hashids or check if the request/response body is related to a Hasheable 
DTO class that also might contain a @Hashids annotation.

When calling the endpoint ```http://localhost:8080/students```, instead of returning the json below:

```json
[
    {
        "id": 1,
        "firstName": "Michael",
        "lastName": "Jordan",
        "email": "michael.jordan@nba.com"
    },
    {
        "id": 2,
        "firstName": "Magic",
        "lastName": "Johnson",
        "email": "magic.johnson@nba.com"
    },
    {
        "id": 3,
        "firstName": "Larry",
        "lastName": "Bird",
        "email": "larry.bird@nba.com"
    }
]
```
The AOP will return a collection of students with the id encoded:
```json
[
    {
        "id": "STD-KZB5B18Y",
        "firstName": "Michael",
        "lastName": "Jordan",
        "email": "michael.jordan@nba.com"
    },
    {
        "id": "STD-M6Q8VW5N",
        "firstName": "Magic",
        "lastName": "Johnson",
        "email": "magic.johnson@nba.com"
    },
    {
        "id": "STD-3KW56K8V",
        "firstName": "Larry",
        "lastName": "Bird",
        "email": "larry.bird@nba.com"
    }
]
```
