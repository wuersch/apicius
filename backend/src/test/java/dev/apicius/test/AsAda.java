package dev.apicius.test;

import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A valid authenticated designer for tests where the identity itself is incidental.
 * Tests whose claims are the point (e.g. provisioning) keep their own explicit blocks.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@TestSecurity(user = "sub-ada")
@OidcSecurity(claims = {
        @Claim(key = "sub", value = "sub-ada"),
        @Claim(key = "name", value = "Ada Lovelace"),
        @Claim(key = "email", value = "ada@example.com"),
        @Claim(key = "email_verified", value = "true")
})
public @interface AsAda {
}
