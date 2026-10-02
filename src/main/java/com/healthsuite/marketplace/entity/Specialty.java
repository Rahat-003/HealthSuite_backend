package com.healthsuite.marketplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "specialties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Specialty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "name_bn", nullable = false, length = 150)
    private String nameBn;

    @Column(name = "desc_en", nullable = false, length = 300)
    private String descEn;

    @Column(name = "desc_bn", nullable = false, length = 400)
    private String descBn;

    @Column(nullable = false, length = 50)
    private String icon;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
