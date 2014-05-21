package org.obiba.agate.domain;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

public class Contact implements Serializable {

  private static final long serialVersionUID = -3098622168836970902L;

  private String title;

  private String firstName;

  @NotBlank
  private String lastName;

  @Email
  private String email;

  private String phone;

  private boolean dataAccessCommitteeMember;

  private Institution institution;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public boolean isDataAccessCommitteeMember() {
    return dataAccessCommitteeMember;
  }

  public void setDataAccessCommitteeMember(boolean dataAccessCommitteeMember) {
    this.dataAccessCommitteeMember = dataAccessCommitteeMember;
  }

  public Institution getInstitution() {
    return institution;
  }

  public void setInstitution(Institution institution) {
    this.institution = institution;
  }

  public static class Institution implements Serializable {

    private static final long serialVersionUID = -3098622168836970902L;

    @NotNull
    private LocalizedString name;

    private LocalizedString department;

    private Address address;

    public LocalizedString getName() {
      return name;
    }

    public void setName(LocalizedString name) {
      this.name = name;
    }

    public LocalizedString getDepartment() {
      return department;
    }

    public void setDepartment(LocalizedString department) {
      this.department = department;
    }

    public Address getAddress() {
      return address;
    }

    public void setAddress(Address address) {
      this.address = address;
    }

  }

}
