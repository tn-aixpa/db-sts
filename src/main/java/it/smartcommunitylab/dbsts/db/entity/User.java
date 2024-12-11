package it.smartcommunitylab.dbsts.db.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Collection;
import java.util.List;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "db_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // WebIdentity details
    @Column(name = "w_i_issuer")
    private String wIIssuer;

    @Column(name = "w_i_username")
    private String wIUsername;

    @Column(name = "w_i_expires_at")
    private Instant wIExpiresAt;

    // DbUser details
    @Column(name = "db_user_username")
    private String dBUserUsername;

    @Convert(converter = UserRoleConverter.class)
    @Column(name = "db_user_roles")
    private Collection<String> dBUserRoles;

    @Column(name = "db_user_valid_until")
    private Instant dBUserValidUntil;

    private Boolean deleted = false;
}
