package com.novainvest.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "screenshots")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Screenshot {

    @Id
    private String id;

    private String imageUrl;
    private String publicId;
    private LocalDateTime uploadedAt;

    // getters/setters
}