package com.twoseat.i2r;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
class EmployeeController {

    private final EmployeeRepository repository;

    /*
     * WebClient replaces RestTemplate.
     */
    private WebClient webClient;

    EmployeeController(EmployeeRepository repository, WebClient.Builder webClientBuilder) {
        this.repository = repository;
        this.webClient = webClientBuilder.baseUrl("http://localhost:8082").build();
    }

    /*
     * List all employees
     *
     * We need to provide a Flux of Employee, and handily that's exactly what repository.findAll() returns, no extra
     * work required.
     */
    @GetMapping("/employees")
    Flux<Employee> all() { //TODO: Wasn't previously public
        return this.repository.findAll();
    }

    /*
     * Find an employee by ID
     *
     * Again we start with a Flux of Employee, but this time we .switchIfEmpty() to handle no value coming back.
     * This can be because of an optional, as in this case, or just a call that we know may return a Mono.empty()
     */
    @GetMapping("/employees/{id}")
    Mono<Employee> one(@PathVariable Long id) {
        return this.repository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException
                        (HttpStatus.NOT_FOUND, "Could not find employee " + id)));
    }

    /*
     * Find problem records
     *
     * Based on the imperative version it might seem like this will be a little complicated, but it's actually just two
     * lines of code. Because we're not dealing with a collection, but a Flux, anything we remove (or in different
     * circumstances add, say by merging two Fluxes) doesn't change the fact that we're still emitting a Flux. And when
     * we do that, WebFlux turns as many items as turn up into data readable by a browser or other client.
     */
    @GetMapping("/employees/problems")
    Flux<Employee> problems() {
        return this.repository.findAll()
                .filter(this::isProblemData);
    }

    /*
     * Create a new employee, looking up their pension id
     *
     * We need to start our flow with either a Mono or Flux. In this case we've modified the original pensionLookup()
     * to return a Mono, so we can start our flow with that. The only trick then is that the setPensionId() call doesn't
     * return anything, and because each step in a flow must pass something along to the next step we need to pass on
     * an employee.
     */
    @PostMapping("/employees")
    Mono<Employee> newEmployee(@RequestBody Employee newEmployee) {
        return pensionLookup(newEmployee.getName())
                .map(pensionId -> {
                    newEmployee.setPensionId(pensionId);
                    return newEmployee;
                })
                .flatMap(this.repository::save);
    }

    /*
     * Update pension records for all employees
     *
     * The code is basically the same as newEmployee(), but applies a flatMap() to the employees emitted by
     * repository.findAll(). For every employee emitted the code performs the external lookup and sets the pensionId
     * value. Project Reactor takes care of scheduling this work across multiple threads as needed, and bringing the
     * results back into a single Flux. The System.out shows us the names of the threads, based on the number of cores
     * in your machine.
     */
    @GetMapping("/employees/pension")
    Flux<Employee> pension() {
        return this.repository.findAll()
                .flatMap(employee -> pensionLookup(employee.getName())
                        .map(pensionId -> {
                            System.out.println("Thread: " + Thread.currentThread().getName());
                            employee.setPensionId(pensionId);
                            return employee;
                        })
                        .flatMap(this.repository::save));
    }

    /*
     * Converted to use webClient()
     */
    private Mono<String> pensionLookup(String name) {
        return this.webClient.get().uri("/{name}", name)
                .retrieve().bodyToMono(String.class)
                .onErrorResume(e -> Mono.just("")); // If pension server is down return empty string rather than a 500
    }

    /*
     * Does not change - the same imperative code, because the reactive .filter() still only requires a boolean.
     */
    private boolean isProblemData(Employee employee) {
        return (employee.getName().isEmpty()) ||
                employee.getPensionId().isEmpty() ||
                employee.getRole().isEmpty() ||
                Character.isLowerCase(employee.getName().charAt(0));
    }

}