package com.petstore.web.form;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class NewUserForm {

    // Length and the %/* rule come from the legacy UserEJB.ejbCreate().
    @NotBlank @Size(max = 25) @Pattern(regexp = "[^%*\\s]+", message = "{error.username}")
    private String username;

    @NotBlank @Size(min = 4, max = 64)
    private String password;

    private String passwordRepeat;

    // The legacy form had a "Password (Repeat)" box that nothing ever compared.
    @AssertTrue(message = "{error.passwordMismatch}")
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(passwordRepeat);
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username == null ? null : username.trim(); }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPasswordRepeat() { return passwordRepeat; }
    public void setPasswordRepeat(String passwordRepeat) { this.passwordRepeat = passwordRepeat; }
}
