# Imperative to Reactive

## Introduction
This is the code for a talk about converting code from Imperative to Reactive, first delivered at Spring IO 2019 in Barcelona.

The code is a modified version of that used in the talk; the enthusiasm around R2DBC made me feel bad about my simple JPA app, so I've upgraded it to use an experimental version of R2DBC H2 support. You'll see some changes in the `pom.xml`, but an upcoming release of Spring Boot should simplify those greatly (as it already does when using JPA, for example).

While doing that I've also taken the chance to add some detailed comments.

## Running the Demo
The demo is split into two commits. The first is the imperative code, the second contains the reactive version.
For full functionality you'll need a fake pension server to respond to requests. If you have go installed you can save the following to a file called `pensionserver.go` and then run `go run pensionserver.go`:
```
package main

import (
	"fmt"
	"net/http"
)

func main() {
	http.HandleFunc("/", HelloServer)
	http.ListenAndServe(":8082", nil)
}

func HelloServer(w http.ResponseWriter, r *http.Request) {
      fmt.Fprintf(w, "%s-pension", r.URL.Path[1:])
}

```
(Note the reactive version contains an improvement that protects against the pension server being down).

## Getting Started with Reactive
1. Update dependencies from imperative to reactive. Some examples:
    1. `spring-boot-starter-web` &rarr; `spring-boot-starter-webflux` 
    1. `RestTemplate` &rarr; `WebClient`
1. For each method decide whether it needs to be reactive, or should stay imperative. As a guide:
    1. Select a method that needs to be reactive (for example, a `@RequestMapping`) 
    1. Convert that method to reactive (see next point).
    1. As it makes calls to other methods, convert them if you need to. Generally:
        1. Methods that return a value for the next step in the flow, e.g. a customer id, should be reactive.
        1. Methods that return a value to be used inside a step, such as a boolean test or simple string manipulation, can be imperative
        1. Clearly there are (many) exceptions to this!
1. Decide the type that the method should return.
    1. If you want a single value, return a `Mono`. So `Student update(id) {...}` becomes `Mono<Student> update(id) {...}`
    1. If you want multiple values, return a `Flux`. So `List<Vehicle> serviceDue() {...}` becomes `Flux<Vehicle> serviceDue() {...}`
    1. Starting with a Collection doesn't automatically mean you need a `Flux`. For example, you might have an expensive operation to look up office locations. Rather than returning a `Flux<Office>` you might choose to return a `Mono<List<Office>>`, giving you a single reusable `Object` without making the expensive call multiple times (assuming the list of offices doesn't change every minute!)
1. Your reactive flow needs to start with either a `Mono` or a `Flux`. You can get this from a number of places, such as:
    1. Generate a`Mono` or `Flux` using a range of `from` methods on `Mono` and `Flux` (e.g. `Flux.fromIterable()`).
    1. Call a method that returns a `Mono` or `Flux`.

## Tips
+ Use `.block()` sparingly. That's particularly true in the middle of a flow, where it's never right (in fact your IDE will probably warn you about it). The most common valid use is when making calls to an external imperative system where you _need_ a particular value to continue. Its main use is to work with systems that are still imperative.
+ Flows can get quite complicated, so composable methods are perhaps even more useful than in imperative code. For example, the following is much more readable than the inlined equivalent:  
```
    methodReturningFluxOfUsers
        .flatMap(MyClass::lookupUserId)
        .flatMap(MyClass::lookupUserStatus)
        .filter(status -> "Warning".equals(status.getMessage())
``` 
+ `TestSubscriber` is the starting point for testing reactive apps. A simple example:
```
    @Test 
    public void testMultipleValues() { 
        Flux.just("alpha", “bravo")       // Create a flux that should emit two values and complete
            .as(StepVerifier::create)       // StepVerifier handles subscribing for us
            .expectNext(“alpha”)            // Verify that 'alpha' is emitted
            .expectNext(“bravo”)            // Then verify that 'bravo' is emitted
            .expectComplete()               // Finally check that complete is emitted, thereby showing that no other value was emitted
            .verify(Duration.ofSeconds(5));  // Give the method a timeout to protect against the complete not arriving
    }
```
+ While your flow has to start with a `Flux` or `Mono` your method doesn't have to. So you can start with some imperative code to set some variables, for example.
+ Similarly a method can have multiple flows, so long as it only return one. For example, if `executiveBooking` and `regularBooking` both return a `Mono` we can use imperative logic to select the appropriate flow (you can do this reactively as well using `filter`, but often the imperative way is more readable).
```
    private static Mono<String> booking(String name) {
        if ("George Clooney".equals(name)) {
            return executiveBooking(name);
        } else {
            return regularBooking(name);
        }
    }
``` 