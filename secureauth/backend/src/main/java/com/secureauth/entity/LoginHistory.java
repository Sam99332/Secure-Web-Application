package com.secureauth.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "login_history")
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "login_time", nullable = false)
    private Instant loginTime;

    @Column(name = "ip_address", nullable = false, length = 64)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_status", nullable = false, length = 30)
    private LoginStatus loginStatus;

    @PrePersist
    protected void onCreate() {
        if (this.loginTime == null) this.loginTime = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Instant getLoginTime() { return loginTime; }
    public void setLoginTime(Instant loginTime) { this.loginTime = loginTime; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public LoginStatus getLoginStatus() { return loginStatus; }
    public void setLoginStatus(LoginStatus loginStatus) { this.loginStatus = loginStatus; }
}
