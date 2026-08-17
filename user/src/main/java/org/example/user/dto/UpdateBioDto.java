package org.example.user.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBioDto {

    @Size(max = 250, message = "Статус не может превышать 250 символов!")
    private String bio;
}
