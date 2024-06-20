package com.dabsquared.gitlabjenkins.gitlab.hook.model;

import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Date;

/**
 * @author Nikolay Ustinov
 */
@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class NoteObjectAttributes {

    private Integer id;
    private String note;
    private Integer authorId;
    private Integer projectId;
    private Date createdAt;
    private Date updatedAt;
    private String url;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public Integer getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Integer authorId) {
        this.authorId = authorId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NoteObjectAttributes that = (NoteObjectAttributes) o;
        return new EqualsBuilder()
                .append(id, that.id)
                .append(note, that.note)
                .append(projectId, that.projectId)
                .append(authorId, that.authorId)
                .append(createdAt, that.createdAt)
                .append(updatedAt, that.updatedAt)
                .append(url, that.url)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(note)
                .append(projectId)
                .append(authorId)
                .append(createdAt)
                .append(updatedAt)
                .append(url)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("note", note)
                .append("projectId", projectId)
                .append("authorId", authorId)
                .append("createdAt", createdAt)
                .append("updatedAt", updatedAt)
                .append("url", url)
                .toString();
    }
}
