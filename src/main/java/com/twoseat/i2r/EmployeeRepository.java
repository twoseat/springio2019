package com.twoseat.i2r;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

interface EmployeeRepository extends ReactiveCrudRepository<Employee, Long> {

}