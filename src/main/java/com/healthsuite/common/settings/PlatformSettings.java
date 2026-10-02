package com.healthsuite.common.settings;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "platform_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformSettings {

    @Id
    @Builder.Default
    private Long id = 1L;

    @Column(name = "max_doctors_per_consultation", nullable = false)
    @Builder.Default
    private int maxDoctorsPerConsultation = 3;

    @Column(name = "consultation_cancel_wait_hours", nullable = false)
    @Builder.Default
    private int consultationCancelWaitHours = 2;
}
