package com.twoseat.i2r;

import lombok.Data;
import lombok.Generated;
import org.springframework.data.annotation.Id;

/*
 * We're using Lombok here just to cut down the amount of boilerplate.
 */
@Data
class Employee {
    @Id
    @Generated
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