package it.smartcommunitylab.dbsts.db;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private String id;
    private Date createdAt;

    // WebIdentity details
    private String webIssuer;

    private String webUser;

    // DbUser details
    private String dbDatabase;
    private String dbUser;
    private String[] dbRoles;

    private Date dbValidUntil;
}
