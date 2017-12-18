package pojo;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.sql.Timestamp;

@Entity
public class Message {
    private int id;
    private Integer conversationId;
    private String username;
    private String messagaeType;
    private String message;
    private Timestamp createAt;

    @Id
    @Column(name = "id")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "conversationID")
    public Integer getConversationId() {
        return conversationId;
    }

    public void setConversationId(Integer conversationId) {
        this.conversationId = conversationId;
    }

    @Basic
    @Column(name = "username")
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Basic
    @Column(name = "messagaeType")
    public String getMessagaeType() {
        return messagaeType;
    }

    public void setMessagaeType(String messagaeType) {
        this.messagaeType = messagaeType;
    }

    @Basic
    @Column(name = "message")
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Basic
    @Column(name = "createAt")
    public Timestamp getCreateAt() {
        return createAt;
    }

    public void setCreateAt(Timestamp createAt) {
        this.createAt = createAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message1 = (Message) o;

        if (id != message1.id) return false;
        if (conversationId != null ? !conversationId.equals(message1.conversationId) : message1.conversationId != null)
            return false;
        if (username != null ? !username.equals(message1.username) : message1.username != null) return false;
        if (messagaeType != null ? !messagaeType.equals(message1.messagaeType) : message1.messagaeType != null)
            return false;
        if (message != null ? !message.equals(message1.message) : message1.message != null) return false;
        if (createAt != null ? !createAt.equals(message1.createAt) : message1.createAt != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (conversationId != null ? conversationId.hashCode() : 0);
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (messagaeType != null ? messagaeType.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (createAt != null ? createAt.hashCode() : 0);
        return result;
    }
}
