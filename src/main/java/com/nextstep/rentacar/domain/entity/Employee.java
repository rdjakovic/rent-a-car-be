package com.nextstep.rentacar.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing an employee.
 */
@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, exclude = {"user", "branch", "maintenanceRecords"})
public class Employee extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "employee_id", nullable = false, unique = true, length = 50)
    @EqualsAndHashCode.Include
    private String employeeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "position", length = 100)
    private String position;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "salary", precision = 12, scale = 2)
    private BigDecimal salary;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Maintenance> maintenanceRecords = new ArrayList<>();

    public Employee(User user, String employeeId, Branch branch, String position) {
        this.user = user;
        this.employeeId = employeeId;
        this.branch = branch;
        this.position = position;
        this.hireDate = LocalDate.now();
    }
}
