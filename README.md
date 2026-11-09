shiro-unit
=========

JUnit rule to simplify apache shiro unit tests.

## Sample:

### shiro.ini

```ini
[users]
trillian = secret, user

[roles]
admin = *
user = something:*
```

### ShiroUnitTest.java

```java
public class ShiroUnitTest {

  @Test
  @SubjectAware(
    username = "trillian",
    password = "secret",
    configuration = "classpath:path/to/shiro.ini"
  )
  public void testMethod(){
    Subject subject = SecurityUtils.getSubject();

    assertNotNull(subject);
    assertTrue(subject.isAuthenticated());
    assertEquals("trillian", subject.getPrincipal());
  }

  @Rule
  public ShiroRule rule = new ShiroRule();
}
```

### Maven usage 

Artifacts are deployed to [Maven Central](http://search.maven.org). To use, drop this in your pom.xml: 
```xml
<dependency>
  <groupId>com.github.sdorra</groupId>
  <artifactId>shiro-unit</artifactId>
  <version>2.0.0-SNAPSHOT</version>
</dependency>
```

## Shiro compatibility

The 2.x line targets Apache Shiro 3 and requires Java 17 or newer. The public
`com.github.sdorra.shiro.ShiroRule` and `SubjectAware` APIs are unchanged: existing
test imports, class/method annotation merging, custom annotations and
`setSubject(Subject)` calls continue to work. Use shiro-unit 1.x for Shiro 1.

The rule initializes and tears down Shiro on the thread evaluating the test,
including cleanup after authentication or test failures. It uses thread bindings
so tests may still replace their subject with `setSubject`. Do not place the rule
inside an already active `Subject.execute` scope: on Java 25, scoped subjects take
precedence over thread bindings. For work on another thread, submit the task
returned by `subject.associateWith(task)`; the caller's subject is not inherited.

The Maven Wrapper downloads Maven 3.9.12 on first use; a separate Maven installation
is not required. Run the tests with `./mvnw test` (`.\mvnw.cmd test` on Windows).
To verify and install a local snapshot without release signing, run
`./mvnw install -Dgpg.skip=true`. An installed Maven 3.6.3 or newer can also be used.
The snapshot version shown above is for local development; use the published 2.x
version once released.
