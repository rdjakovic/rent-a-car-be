package com.nextstep.rentacar.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a user role in the system.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class Role extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 50)
    @EqualsAndHashCode.Include
    private String name;

    @Column(name = "description")
    private String description;

    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<User> users = new HashSet<>();

    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
