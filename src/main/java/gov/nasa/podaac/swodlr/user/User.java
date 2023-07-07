package gov.nasa.podaac.swodlr.user;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "Users")
public class User {
  @Id
  private UUID id;

  @Column(unique = true)
  private String username;

  @Column(nullable = false)
  public String email;

  @Column(nullable = false)
  public String firstName;

  @Column(nullable = false)
  public String lastName;

  public User() { }

  public User(String username, String email, String firstName, String lastName) {
    id = UUID.randomUUID();
    this.username = username;
    this.email = email;
    this.firstName = firstName;
    this.lastName = lastName;
  }
  
  public UUID getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }
}
