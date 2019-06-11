package com.twoseat.i2r;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RestController
class EmployeeController {

    private final EmployeeRepository repository;

    private RestTemplate restTemplate;

    EmployeeController(EmployeeRepository repository, RestTemplateBuilder restTemplateBuilder) {
        this.repository = repository;
        this.restTemplate = restTemplateBuilder.build();
    }

    // List all employees
    @GetMapping("/employees")
    List<Employee> all() {
        return repository.findAll();
    }

    // Find an employee by ID
    @GetMapping("/employees/{id}")
    Employee one(@PathVariable Long id) {

        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find employee " + id));
    }

    // Find problem records
    @GetMapping("/employees/problems")
    List<Employee> problems() {
        List<Employee> problems = new ArrayList<>();
        List<Employee> candidates = repository.findAll();

        for (Employee employee : candidates) {
            if (isProblemData(employee)) {
                problems.add(employee);
            }
        }

        return problems;
    }

    //Create a new employee, looking up their pension id
    @PostMapping("/employees")
    Employee newEmployee(@RequestBody Employee newEmployee) {
        String name = newEmployee.getName();
        String pensionId = pensionLookup(name);
        newEmployee.setPensionId(pensionId);

        return repository.save(newEmployee);
    }

    // Update pension records for all employees
    @GetMapping("/employees/pension")
    List<Employee> pension() {
        List<Employee> all = repository.findAll();

        for (Employee employee : all) {
            System.out.println("Thread: " + Thread.currentThread().getName());
            String name = employee.getName();
            String pensionId = pensionLookup(name);
            employee.setPensionId(pensionId);
        }

        return repository.saveAll(all);
    }

    private String pensionLookup(String name) {
        return restTemplate.getForObject(String.format("http://localhost:8082/%s", name), String.class);
    }

    private boolean isProblemData(Employee employee) {
        return (employee.getName().isEmpty()) ||
                employee.getPensionId().isEmpty() ||
                employee.getRole().isEmpty() ||
                Character.isLowerCase(employee.getName().charAt(0));
    }

}