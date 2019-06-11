package com.twoseat.i2r;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Data
@Entity
class Employee {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private String pensionId;
    private String role;

    Employee() {
    }

    Employee(String name, String pensionId, String role) {
        this.name = name;
        this.pensionId = pensionId;
        this.role = role;
    }

}