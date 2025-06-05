package com.example.ocean.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Column(name = "user_code")
    private String userCode;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "provider")
    private String provider;
}
