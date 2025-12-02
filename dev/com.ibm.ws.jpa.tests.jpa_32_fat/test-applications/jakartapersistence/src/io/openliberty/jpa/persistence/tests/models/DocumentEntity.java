/**
 * 
 */
package io.openliberty.jpa.persistence.tests.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

@Entity
public class DocumentEntity {

    @Id
    private Long id;

    @Lob
    private String content;

    public DocumentEntity() {}

    public DocumentEntity(Long id, String content) {
        this.id = id;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }
}
