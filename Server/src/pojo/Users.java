package pojo;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Users {
    private String username;
    private String pass;
    private String nickname;

    public Users(){};
    public Users(String username, String pass, String nickname){
        this.username=username;
        this.pass=pass;
        this.nickname=nickname;
    }
    @Id
    @Column(name = "username")
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Basic
    @Column(name = "pass")
    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    @Basic
    @Column(name = "nickname")
    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Users users = (Users) o;

        if (username != null ? !username.equals(users.username) : users.username != null) return false;
        if (pass != null ? !pass.equals(users.pass) : users.pass != null) return false;
        if (nickname != null ? !nickname.equals(users.nickname) : users.nickname != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = username != null ? username.hashCode() : 0;
        result = 31 * result + (pass != null ? pass.hashCode() : 0);
        result = 31 * result + (nickname != null ? nickname.hashCode() : 0);
        return result;
    }
}
