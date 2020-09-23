# hal

Hal is a library for symmetrical encryption. Hal is implemented in Clojure, designed
to be used from either Clojure or Java.

## Usage

### Clojure

``` clj
[hal "0.1.0"]
```

For symmetrical encryption needs, see the `encrypt` and `decrypt` functions in the
`hal.core` namespace.

For web-tokens, see the `hal.token` namespace, in particular the `mint` and
`decrypt` functions.

NOTE: The input data for a token must be a {} with only `String` keys, and values must
be `String`s, `number`s, `java.util.Date`s, or `org.joda.time.DateTime`s.


Example token mint and decrypt at REPL:

```clj
(hal.token/mint {"a" 1})
=> "WgSBV2nutwxN0FqVK038Go69YC8Er9UZfwn6HJnwNpU="
(hal.token/decrypt "WgSBV2nutwxN0FqVK038Go69YC8Er9UZfwn6HJnwNpU=")
=> {"a" 1}
```

### Java

Maven dependency information:

    <dependency>
      <groupId>org.purefn</groupId>
      <artifactId>hal</artifactId>
      <version>0.1.0</version>
    </dependency>


A Java class `org.purefn.Token` is exposed from the `hal.token` namespace, it has
two *static* methods: `.mint(Map<String, ?>)` and `.decrypt(String)`

NOTE: The input data for a token must be a `Map` with only `String` keys, and values must be `String`s, `number`s, `java.util.Date`s, or `org.joda.time.DateTime`s.

```java
import org.purefn.Token;

public class Main {
  public static void main(String[] args) {

      HashMap<String, ?> tokenData = new HashMap<>();
      tokenData.put("id1", "12345");
      tokenData.put("id2", "67890");


      String minted = org.purefn.Token.mint(tokenData); //calling the static method mint

      System.out.println(minted);

      HashMap<String, ?> decrypted = org.purefn.Token.decrypt(minted); //calling the static method decrypt

      System.out.println(decrypted);
  }
}
```

## Development

### Specs

Several specs for tokens and the token fns exist in the `hal.token` namespace.

### Tests

Check out the `hal.core-test` namespace.

## Sql UUIDs (type 6 UUID)

```java
import java.util.UUID;
import org.purefn.Type6UUID;

public class HalUUIDTest {

  public static void main(String[] args) {
      UUID uuid = Type6UUID.type6("my_table", 255);
      System.out.println(uuid);

      String table = Type6UUID.table(uuid);
      System.out.println(table);

      Integer id = Type6UUID.id(uuid);
      System.out.println(id);
  }
}
```
