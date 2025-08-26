package com.nextstep.rentacar.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a rental branch location.
 */
@Entity
@Table(name = "branches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, exclude = {"cars", "employees", "pickupReservations", "dropoffReservations"})
public class Branch extends BaseEntity {

    @Column(name = "name", nullable = false)
    @EqualsAndHashCode.Include
    private String name;

    @Column(name = "address", nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "opening_hours", columnDefinition = "TEXT")
    private String openingHours;

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Car> cars = new ArrayList<>();

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Employee> employees = new ArrayList<>();

    @OneToMany(mappedBy = "pickupBranch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Reservation> pickupReservations = new ArrayList<>();

    @OneToMany(mappedBy = "dropoffBranch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Reservation> dropoffReservations = new ArrayList<>();

    public Branch(String name, String address, String city, String country) {
        this.name = name;
        this.address = address;
        this.city = city;
        this.country = country;
    }
}
