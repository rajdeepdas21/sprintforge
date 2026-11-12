
package com.sprintforge.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "workspaces")
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    public Workspace() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getOwnerId() {
        return ownerId;
    }
    public void setName(String name) {
    this.name = name;
}

public void setOwnerId(Long ownerId) {
    this.ownerId = ownerId;
}
}
