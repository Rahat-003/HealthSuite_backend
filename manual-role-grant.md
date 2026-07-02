# Manually Granting Roles (ROLE_ADMIN / ROLE_SUPPORT)

There is no seeded admin user — the `V1` migration only inserts the role rows.
To make an account an admin or support agent, insert a row into the `user_roles`
join table directly in Postgres.

## 1. Connect to the database

The Postgres container is `healthsuite-postgres`, exposed on host port **5450**
(container port 5432). Credentials come from `DATABASE_USERNAME` /
`DATABASE_PASSWORD` in your `.env`.

```bash
# Inside the container (recommended)
docker exec -it healthsuite-postgres psql -U <DATABASE_USERNAME> -d healthsuite

# Or from the host
psql -h localhost -p 5450 -U <DATABASE_USERNAME> -d healthsuite
```

## 2. Check the available roles

```sql
SELECT * FROM roles;
```

Seeded roles: `ROLE_USER`, `ROLE_PREMIUM`, `ROLE_SUPPORT`, `ROLE_ADMIN`, `ROLE_DOCTOR`.

## 3. Grant a role to a user

Replace the email with the account you want to promote.

**Grant ROLE_ADMIN:**

```sql
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'your@email.com'
  AND r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;
```

**Grant ROLE_SUPPORT:**

```sql
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'your@email.com'
  AND r.name = 'ROLE_SUPPORT'
ON CONFLICT DO NOTHING;
```

**Both at once:**

```sql
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'your@email.com'
  AND r.name IN ('ROLE_ADMIN', 'ROLE_SUPPORT')
ON CONFLICT DO NOTHING;
```

`ON CONFLICT DO NOTHING` makes the statement safe to re-run — it silently skips
roles the user already has.

### One-liner without opening a psql session

```bash
docker exec -it healthsuite-postgres psql -U <DATABASE_USERNAME> -d healthsuite -c \
  "INSERT INTO user_roles (user_id, role_id)
   SELECT u.id, r.id FROM users u, roles r
   WHERE u.email = 'your@email.com' AND r.name = 'ROLE_ADMIN'
   ON CONFLICT DO NOTHING;"
```

## 4. Verify

```sql
SELECT u.email, r.name
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
WHERE u.email = 'your@email.com';
```

## 5. Revoke a role

```sql
DELETE FROM user_roles ur
USING users u, roles r
WHERE ur.user_id = u.id
  AND ur.role_id = r.id
  AND u.email = 'your@email.com'
  AND r.name = 'ROLE_ADMIN';
```

## Important: log out and back in

Roles are embedded in the JWT and in the user object returned at login. After
changing roles in the database, the user must **log out and log in again** for
the new role to take effect — both for backend authorization
(`/api/admin/**` requires `ROLE_ADMIN`) and for frontend UI like the
Admin → Doctor Approvals sidebar entry.
