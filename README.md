# Database STS

A token exchange service to obtain (temporary) db credentials from a web identity via JWT.

## Configure

You need to set both a valid issuer for JWT token validation and a DB connection.

Currently supported:

- Postgresql

```
  # credentials configuration
  credentials:
    duration: ${STS_CREDENTIALS_DURATION:28800}
    roles: ${STS_CREDENTIALS_ROLES:}
    password-length: ${STS_CREDENTIALS_PWD_LENGTH:12}

  # optional basic auth for client requests
  client:
    client-id: ${STS_CLIENT_ID:}
    client-secret: ${STS_CLIENT_SECRET:}

  # jwt configuration
  jwt:
    issuer-uri: ${STS_JWT_ISSUER_URI:}
    audience: ${STS_JWT_AUDIENCE:sts}
    claim: ${STS_JWT_CLAIM:roles}

  #db configuration
  password: ${JDBC_PASS:}
  username: ${JDBC_USER:sts}
  url: ${JDBC_URL:}
  platform: ${JDBC_PLATFORM:postgresql}


```

Beware: not production-ready.
