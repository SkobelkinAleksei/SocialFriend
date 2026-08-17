package org.example.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatFileAttachment {
    @Column(name = "file_url", length = 255)
    private String url;

    @Column(name = "file_name", length = 255)
    private String name;

    @Column(name = "file_size")
    private Long size;

    @Column(name = "file_mime", length = 120)
    private String mimeType;
}
