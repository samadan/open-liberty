package io.openliberty.jpa.persistence.tests.models;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Simple entity with an embedded object to demonstrate PostgreSQL type mismatch error
 * when setting an embedded object to null via JPQL.
 */
@Entity
public class Employee {
    @GeneratedValue
    @Id
    public Long id;

    public String name;

    @Embedded
    public EmployeeInfo info;

    @Embeddable
    public static class EmployeeInfo {
        public Integer salary;
        public String bloodGroup;

        public static EmployeeInfo of(int salary, String bloodGroup) {
            EmployeeInfo inst = new EmployeeInfo();
            inst.salary = salary;
            inst.bloodGroup = bloodGroup;
            return inst;
        }
    }

    public static Employee of(String name, int salary, String bloodGroup) {
        Employee employee = new Employee();
        employee.name = name;
        employee.info = EmployeeInfo.of(salary, bloodGroup);
        return employee;
    }
}
